package amuhak.av1.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ConversionService {

    public final Logger logger = org.slf4j.LoggerFactory.getLogger(ConversionService.class);

    public int convertToAv1(Path oldPath, String hash) throws IOException {
        AtomicBoolean isDone = new AtomicBoolean(false); // Is the conversion done?

        Path newPath = Paths.get("downloads/" + hash + ".mp4"); // Path to the new file

        Files.createDirectories(newPath.getParent());

        Converter.conversionStatus.put(hash + ".mp4", isDone); // Add the conversion status to the map

        Job job = new Job(oldPath, newPath, isDone); // Create a job

        Converter.addJob(job); // Submit the job to the executor

        // Wait for the conversion to finish
        while (!isDone.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Files.delete(oldPath);    // Delete the old file

        Path outputPath = Paths.get("downloads/" + hash + "/" + "temp.mp4");
        Files.createDirectories(outputPath.getParent());   // create directories "downloads/{hash}"

        // Split the file into chunks (90mb each)
        long noOfChunks = newPath.toFile().length() / (90 * 1024 * 1024) + 1;
        long realNo = splitFile(newPath, outputPath.getParent(), 90 * 1024 * 1024);
        if (realNo != noOfChunks) {
            logger.error("Error splitting file expected: {} got: {}", noOfChunks, realNo);
        }
        Files.delete(newPath);  // Delete the new file that was split
        return (int) realNo;
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
}
