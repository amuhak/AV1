package amuhak.av1.service;


import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class Job implements Runnable {
    private final String inputFilePath;
    private final String outputFilePath;
    public final Logger logger = org.slf4j.LoggerFactory.getLogger(Job.class);
    private final AtomicBoolean done;


    public Job(String inputFilePath, String outputFilePath, AtomicBoolean done) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.done = done;
    }

    @Override
    public void run() {
        String[] command =
                {"ffmpeg", "-nostdin", "-i", inputFilePath, "-c:v", "libsvtav1", "-c:a", "libopus", outputFilePath,
                        "-y"};
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
        private InputStream inputStream;
        private java.util.function.Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, java.util.function.Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (java.util.Scanner scanner = new java.util.Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    consumer.accept(scanner.nextLine());
                }
            }
        }
    }
}
