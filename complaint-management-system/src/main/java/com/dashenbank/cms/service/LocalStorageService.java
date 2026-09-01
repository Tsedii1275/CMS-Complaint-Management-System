package com.dashenbank.cms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetPath = Paths.get(uploadDir).resolve(storedFileName).toAbsolutePath();
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return storedFileName;
    }

    @Override
    public Resource loadFileAsResource(String storedFileName) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(storedFileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("File not found or not readable: " + storedFileName);
        }
    }

    @Override
    public void deleteFile(String storedFileName) {
        try {
            Path path = Paths.get(uploadDir).resolve(storedFileName).normalize();
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Could not delete physical file {}: {}", storedFileName, e.getMessage());
        }
    }
}
