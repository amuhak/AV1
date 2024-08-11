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
        logger.info("User accessed the index page");
        return "index";
    }

    @PostMapping("/upload-chunk")
    ResponseEntity<?> uploadChunk(@RequestParam("Hash") String hash, @RequestParam("FileName") String fileName,
                                  @RequestParam("Chunk") MultipartFile chunk, @RequestParam("Index") int index,
                                  @RequestParam("TotalChunks") int totalChunks) {
        logger.info("Uploading chunk: {} of {} for hash {}", index, totalChunks, hash);
        if (chunk.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Chunk is empty"));
        }
        try {
            Path chunkPath = Paths.get("uploads/" + hash + "/" + index + ".part");
            Files.createDirectories(chunkPath.getParent());  // create directories "uploads/hash"
            Files.write(chunkPath, chunk.getBytes());
            return ResponseEntity.ok().body(Map.of("Good", "Good"));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload chunk: " + e.getMessage()));
        }
    }

    @PostMapping("/combine")
    private ResponseEntity<?> combineChunks(@RequestParam("FileName") String fileName,
                                            @RequestParam("NoOfChunks") int NoOfChunks,
                                            @RequestParam("Hash") String hash) {
        logger.info("Combining chunks for hash: {}", hash);
        String extension = fileName.substring(fileName.lastIndexOf("."));
        Path outputPath = Paths.get("uploads" + "/" + hash + extension);
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            for (int i = 0; i < NoOfChunks; i++) {
                Path chunkPath = Paths.get("uploads/" + hash + "/" + i + ".part");
                Files.copy(chunkPath, out);
            }
            FileUtils.deleteDirectory(Paths.get("uploads/" + hash).toFile());
        } catch (IOException e) {
            logger.error("Failed to combine chunks: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to combine chunks" + e.getMessage()));
        }
        return ResponseEntity.ok().body(Map.of("success", "Chunks combined successfully"));
    }


    @PostMapping("/process")
    ResponseEntity<?> process(@RequestParam("Hash") String hash, @RequestParam("FileName") String fileName) {
        logger.info("Processing file: {}", hash);
        try {
            // return ResponseEntity.ok().body(Map.of("downloadUrl", "downloads/" + hash + ".mp4"));
            Thread.sleep(1000);
            String extention = fileName.substring(fileName.lastIndexOf("."));
            Path file = Paths.get("uploads/" + hash + extention);
            int noOfParts = conversionService.convertToAv1(file, hash);
            return ResponseEntity.ok().body(Map.of("noOfParts", noOfParts));
        } catch (Exception e) {
            logger.error("Failed to process file: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process file: " + e.getMessage()));
        }
    }

    @GetMapping("/downloads/{hash}/{partNo}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String hash, @PathVariable int partNo) throws IOException {
        logger.info("Downloading part: {} for hash: {}", partNo, hash);
        Path filePath = Paths.get("downloads" + "/" + hash + "/" + partNo + ".part");
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
