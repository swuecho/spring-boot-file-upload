package com.example.fileupload;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<Path> createdFiles = new HashSet<>();

    @AfterEach
    void cleanup() throws IOException {
        for (Path path : createdFiles) {
            Files.deleteIfExists(path);
        }
        createdFiles.clear();
    }

    @Test
    void uploadAndDownloadZipByReportId() throws Exception {
        byte[] zipContent = createZip("hello.txt", "hello world");
        MockMultipartFile file = new MockMultipartFile("file", "sample.zip", "application/zip", zipContent);
        String reportId = "report-123";

        MvcResult uploadResult = mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .param("reportId", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String savedPath = responseJson.get("path").asText();
        Path storedFile = Paths.get(savedPath);
        createdFiles.add(storedFile);

        mockMvc.perform(get("/api/download/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(zipContent));
    }

    private byte[] createZip(String entryName, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
