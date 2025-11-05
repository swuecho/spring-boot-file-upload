package com.example.fileupload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final Path TARGET_DIRECTORY = Paths.get("/tmp");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam("reportId") String reportId) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }

        if (!isZipFile(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ZIP files are supported");
        }

        if (!StringUtils.hasText(reportId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportId is required");
        }

        try {
            Files.createDirectories(TARGET_DIRECTORY);

            logger.info("Received upload request for reportId={} filename={}", reportId, file.getOriginalFilename());

            String originalName = StringUtils.hasText(file.getOriginalFilename())
                    ? Objects.requireNonNull(file.getOriginalFilename())
                    : "upload.zip";

            String sanitizedReportId = reportId.replaceAll("[^a-zA-Z0-9-_]", "_");
            String sanitizedFilename = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetFile = TARGET_DIRECTORY.resolve(sanitizedReportId + "-" + sanitizedFilename);

            file.transferTo(targetFile);

            String savedPath = targetFile.toAbsolutePath().toString();
            logger.info("Saved ZIP for reportId={} to {}", reportId, savedPath);

            return ResponseEntity.ok(Map.of("path", savedPath));
        } catch (IOException ex) {
            logger.error("Failed to store ZIP for reportId={}", reportId, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file", ex);
        }
    }

    private boolean isZipFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (ZIP_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            return true;
        }

        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".zip");
    }
}
