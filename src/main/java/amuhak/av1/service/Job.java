package amuhak.av1.service;


import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Consumer;

public class Job implements Runnable {
    private final Path inputFilePath;
    private final Path outputFilePath;
    public final Logger logger = org.slf4j.LoggerFactory.getLogger(Job.class);
    private final Converter.Status done;
    private final String hash;


    public Job(Path inputFilePath, Path outputFilePath, Converter.Status done, String hash) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.done = done;
        this.hash = hash;
    }

    @Override
    public void run() {
        String[] command =
                {"ffmpeg", "-nostdin", "-i", inputFilePath.toAbsolutePath().toString(), "-c:v", "libsvtav1", "-c:a",
                        "libopus", outputFilePath.toAbsolutePath().toString(), "-y"};
        logger.info("Starting the process: {}", Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            Process process = processBuilder.start();
            logger.info("Process started");

            // Capture and log output from the process
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

            outputGobbler.start();
            errorGobbler.start();

            int exitCode = process.waitFor();
            outputGobbler.join();
            errorGobbler.join();

            logger.info("Process finished with exit code: {}", exitCode);
        } catch (IOException e) {
            logger.error("Failed to start the process: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Process interrupted: {}", e.getMessage());
        }
        done.doneInternal().set(true);

        try {
            Files.delete(inputFilePath);    // Delete the old file
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path outputPath = Paths.get("downloads/" + hash + "/" + "temp.mp4");
        try {
            Files.createDirectories(outputPath.getParent());   // create directories "downloads/{hash}"
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long noOfChunks = outputFilePath.toFile().length() / (90 * 1024 * 1024) + 1;
        long realNo = 0;
        try {
            realNo = splitFile(outputFilePath, outputPath.getParent(), 90 * 1024 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (realNo != noOfChunks) {
            logger.error("Error splitting file expected: {} got: {}", noOfChunks, realNo);
        }
        done.parts().set((int) realNo);     // Set the number of parts the file was split into
        done.done().set(true);
        try {
            Files.delete(outputFilePath);     // Delete the new file that was split
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public long splitFile(Path inputPath, Path outputPath, int chunkSize) throws IOException {
        byte[] buffer = new byte[chunkSize];
        int chunkNumber = 0;
        try (InputStream inputStream = Files.newInputStream(inputPath)) {
            while (inputStream.available() > 0) {
                Path chunkPath = Paths.get(outputPath.toString() + "/" + chunkNumber + ".part");
                try (OutputStream outputStream = Files.newOutputStream(chunkPath)) {
                    int bytesRead = inputStream.read(buffer);
                    outputStream.write(buffer, 0, bytesRead);
                }
                chunkNumber++;
            }
        }
        return chunkNumber;
    }

    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (Scanner sc = new Scanner(inputStream)) {
                while (sc.hasNextLine()) {
                    consumer.accept(sc.nextLine());
                }
            }
        }
    }
}
