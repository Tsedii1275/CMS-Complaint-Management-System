package com.dashenbank.cms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path uploadRoot;

    public LocalStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        Files.createDirectories(uploadRoot);
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")
                || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + fileExtension;
        Path targetPath = resolveSafe(storedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return storedFileName;
    }

    @Override
    public Resource loadFileAsResource(String storedFileName) throws IOException {
        Path filePath = resolveSafe(storedFileName);
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new IOException("File not found or not readable");
    }

    @Override
    public void deleteFile(String storedFileName) {
        try {
            Files.deleteIfExists(resolveSafe(storedFileName));
        } catch (Exception e) {
            log.warn("Could not delete physical file {}: {}", storedFileName, e.getMessage());
        }
    }

    private Path resolveSafe(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()
                || storedFileName.contains("..")
                || storedFileName.contains("/")
                || storedFileName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stored file name");
        }
        Path resolved = uploadRoot.resolve(storedFileName).normalize().toAbsolutePath();
        if (!resolved.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Path traversal rejected");
        }
        return resolved;
    }
}
