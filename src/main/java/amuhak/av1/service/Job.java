package amuhak.av1.service;


import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Job implements Runnable {
    private final Path inputFilePath;
    private final Path outputFilePath;
    public final Logger logger = org.slf4j.LoggerFactory.getLogger(Job.class);
    private final AtomicBoolean done;


    public Job(Path inputFilePath, Path outputFilePath, AtomicBoolean done) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.done = done;
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
        done.set(true);
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
