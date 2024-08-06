package amuhak.av1.service;


import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class Job implements Runnable {
    private final String inputFilePath;
    private final String outputFilePath;
    public final Logger logger = getLogger(Job.class);
    private final AtomicBoolean done;


    public Job(String inputFilePath, String outputFilePath, AtomicBoolean done) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.done = done;
    }

    @Override
    public void run() {
        String[] command = {"ffmpeg", "-nostdin", "-i", inputFilePath, "-c:v", "libsvtav1", outputFilePath};
        logger.info("Starting the process: {}", Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            Process process = processBuilder.start();
            logger.info("Process started");
            int exitCode = process.waitFor();
            logger.info("Process finished with exit code: " + exitCode);
        } catch (IOException e) {
            logger.error("Failed to start the process: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Process interrupted: {}", e.getMessage());
        }
        done.set(true);
    }
}
