package amuhak.av1.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConversionService {

    public final Logger logger = org.slf4j.LoggerFactory.getLogger(ConversionService.class);

    public void convertToAv1(Path oldPath, String hash) throws IOException {
        AtomicBoolean isDoneInternal = new AtomicBoolean(false); // Is the conversion done?
        AtomicBoolean isDone = new AtomicBoolean(false); // Is the conversion done?
        AtomicInteger parts = new AtomicInteger(0); // Number of parts the file was split into
        Converter.Status status = new Converter.Status(isDoneInternal, isDone, parts); // Create a status object

        Path newPath = Paths.get("downloads/" + hash + ".mp4"); // Path to the new file

        Files.createDirectories(newPath.getParent());

        Converter.conversionStatus.put(hash + ".mp4", status); // Add the conversion status to the map

        Job job = new Job(oldPath, newPath, status, hash); // Create a job

        Converter.addJob(job); // Submit the job to the executor
    }
}
