package com.accounting.ocr.service;

import com.accounting.ocr.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.max-size:10485760}")
    private long maxFileSize;

    public String storeFile(MultipartFile file) {
        try {
            validateFile(file);

            String fileId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = fileId + (extension.isEmpty() ? "" : "." + extension);

            // Создаем директорию если не существует
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadDir);
            }

            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {} -> {}", originalFilename, storedFilename);

            return fileId;

        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new DocumentProcessingException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Path getFilePath(String fileId) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            return Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith(fileId))
                    .findFirst()
                    .orElseThrow(() -> new DocumentProcessingException("File not found: " + fileId));
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to locate file: " + fileId, e);
        }
    }

    public void deleteFile(String fileId) {
        try {
            Path filePath = getFilePath(fileId);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", fileId);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileId, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new DocumentProcessingException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new DocumentProcessingException(
                    String.format("File size exceeds maximum allowed size: %d bytes (max: %d bytes)",
                            file.getSize(), maxFileSize)
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new DocumentProcessingException(
                    "Invalid file type. Only images and PDF files are supported. Received: " + contentType
            );
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}