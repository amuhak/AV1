// AV1\src\main\java\amuhak\av1\controller\ConversionController.java
package amuhak.av1.controller;

import amuhak.av1.service.ConversionService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ConversionController {

    @Autowired
    private ConversionService conversionService;
    public final Logger logger = org.slf4j.LoggerFactory.getLogger(ConversionController.class);

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @PostMapping("/upload")
    ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        logger.info("Unvalidated File uploaded: {}", file.getOriginalFilename());
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("The File You uploaded is empty/invalid");
            return ResponseEntity.status(400).body("The File You uploaded is empty/invalid");
        }
        try {
            logger.info("Valid File uploaded: {}", file.getOriginalFilename());
            Path[] files = new Path[1];
            String convertedFilePath = conversionService.convertToAv1(file, files);
            redirectAttributes.addFlashAttribute("message",
                    "Conversion successful. File available at: " + convertedFilePath);
            Path path = Paths.get(convertedFilePath);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new IOException("Could not read the file: " + convertedFilePath);
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "Failed to convert file: " + e.getMessage());
            return ResponseEntity.status(500).body("Failed to convert file: " + e.getMessage());
        }
    }
}
