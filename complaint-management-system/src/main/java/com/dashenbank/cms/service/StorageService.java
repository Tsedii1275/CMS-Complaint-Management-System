package com.dashenbank.cms.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    /**
     * Store file and return unique physical storage identifier.
     */
    String storeFile(MultipartFile file) throws IOException;

    /**
     * Load stored file as Resource.
     */
    Resource loadFileAsResource(String storedFileName) throws IOException;

    /**
     * Delete stored file from physical storage.
     */
    void deleteFile(String storedFileName);
}
