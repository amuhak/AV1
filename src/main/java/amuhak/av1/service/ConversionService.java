package amuhak.av1.service;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ConversionService {

    public String convertToAv1(MultipartFile file, Path[] files) throws IOException {
        String fileName = file.getOriginalFilename();
        assert fileName != null;
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        ByteSource byteSource = ByteSource.wrap(file.getBytes());
        HashCode hc = byteSource.hash(Hashing.sha256());
        String newName = hc.toString();
        Path path = Paths.get("uploads/" + newName + fileExtension);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        Path newPath = Paths.get("uploads/" + newName + "_av1_" + ".mp4");
        AtomicBoolean isDone = new AtomicBoolean(false);
        Job job = new Job(path.toAbsolutePath().toString(), newPath.toAbsolutePath().toString(), isDone);
        files[0] = newPath;
        Converter.addJob(job);
        while (!isDone.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Files.delete(path);
        return newPath.toAbsolutePath().toString();
    }
}
