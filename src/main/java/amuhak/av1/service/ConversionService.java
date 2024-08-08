package amuhak.av1.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ConversionService {

    public String convertToAv1(File file, String hash) throws IOException {
        String extension = file.getName().substring(file.getName().lastIndexOf('.'));
        Path newPath = Paths.get("uploads/" + hash + ".mp4");
        AtomicBoolean isDone = new AtomicBoolean(false);
        Job job = new Job(file.getPath(), newPath.toAbsolutePath().toString(), isDone);
        Converter.addJob(job);
        while (!isDone.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Files.delete(file.toPath());
        return newPath.toAbsolutePath().toString();
    }
}
