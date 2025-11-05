package com.example.fileupload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final Path TARGET_DIRECTORY = Paths.get("/tmp");
    private static final String SAFE_SEGMENT_REGEX = "[^a-zA-Z0-9-_]";
    private static final String SAFE_FILENAME_REGEX = "[^a-zA-Z0-9._-]";

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

            String sanitizedReportId = sanitizeReportId(reportId);
            String sanitizedFilename = originalName.replaceAll(SAFE_FILENAME_REGEX, "_");
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

    @GetMapping(value = "/download/{reportId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadZip(@PathVariable("reportId") String reportId) {
        if (!StringUtils.hasText(reportId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportId is required");
        }

        String sanitizedReportId = sanitizeReportId(reportId);

        if (!Files.exists(TARGET_DIRECTORY)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No files available for download");
        }

        try (Stream<Path> files = Files.list(TARGET_DIRECTORY)) {
            Path match = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(sanitizedReportId + "-"))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

            FileSystemResource resource = new FileSystemResource(match);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + match.getFileName().toString() + "\"")
                    .contentLength(Files.size(match))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException ex) {
            logger.error("Failed to load ZIP for reportId={}", reportId, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", ex);
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

    private String sanitizeReportId(String reportId) {
        return reportId.replaceAll(SAFE_SEGMENT_REGEX, "_");
    }
}
