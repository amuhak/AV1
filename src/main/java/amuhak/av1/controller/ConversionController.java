// AV1\src\main\java\amuhak\av1\controller\ConversionController.java
package amuhak.av1.controller;

import amuhak.av1.service.ConversionService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class ConversionController {

    @Autowired
    private ConversionService conversionService;
    public final Logger logger = org.slf4j.LoggerFactory.getLogger(ConversionController.class);

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @GetMapping("/invalid")
    public String invalid() {
        return "invalid";
    }

    @PostMapping("/upload-chunk")
    ResponseEntity<?> uploadChunk(@RequestParam("Hash") String hash, @RequestParam("FileName") String fileName,
                                  @RequestParam("Chunk") MultipartFile chunk, @RequestParam("Index") int index,
                                  @RequestParam("TotalChunks") int totalChunks) {
        if (chunk.isEmpty()) {
            // redirect to invalid (/invalid) page if file is empty
            return ResponseEntity.status(400).body("The File You uploaded is empty/invalid");
        }
        try {
            Path chunkPath = Paths.get("uploads/" + hash + "/" + index + ".part");
            Files.createDirectories(chunkPath.getParent());
            Files.write(chunkPath, chunk.getBytes());
            if (index == totalChunks) {
                Path outputPath = Paths.get("uploads/" + fileName);
                combineChunks(hash, totalChunks, outputPath);
                return processCompleteFile(outputPath.toFile(), hash);
            }
            return ResponseEntity.ok().body(Map.of("Good", "Good"));
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to process chunk: " + e.getMessage());
        }
    }


    private void combineChunks(String filename, int totalChunks, Path outputPath) throws IOException {
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            for (int i = 0; i <= totalChunks; i++) {
                Path chunkPath = Paths.get("uploads/" + filename + "/" + i + ".part");
                Files.copy(chunkPath, out);
                Files.delete(chunkPath);
            }
            FileUtils.deleteDirectory(Paths.get("uploads/" + filename).toFile());
        }
    }


    @PostMapping("/upload")
    ResponseEntity<?> processCompleteFile(File file, String hash) {
        logger.info("Unvalidated File uploaded: {}", file.getName());
        if (!file.exists()) {
            return ResponseEntity.status(400).body("The File You uploaded is empty/invalid");
        }
        try {
            logger.info("Valid File uploaded: {}", file.getName());
            String convertedFilePath = conversionService.convertToAv1(file, hash);
            Path path = Paths.get(convertedFilePath);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().body(Map.of("downloadUrl", "/uploads/" + hash + ".mp4"));
            } else {
                throw new IOException("Could not read the file: " + convertedFilePath);
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to convert file: " + e.getMessage());
        }
    }

    @GetMapping("/uploads/{url}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String url) throws IOException {
        Path filePath = Paths.get("uploads/" + url);
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
