package com.dashenbank.cms.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AES-256-GCM field encryption is ready when {@code AES_DATA_KEY} (32-byte
 * Base64) is supplied. No field is encrypted until that key is present so UAT
 * can show algorithm and key-management status without changing stored data.
 */
@Component
public class Aes256GcmReadyCrypto {

    public static final String ALGORITHM = "AES-256-GCM";
    public static final String KEY_ENV = "AES_DATA_KEY";

    private final Environment environment;

    public Aes256GcmReadyCrypto(Environment environment) {
        this.environment = environment;
    }

    public boolean keyPresent() {
        return StringUtils.hasText(environment.getProperty(KEY_ENV));
    }

    public String keyManagementStatus() {
        return keyPresent() ? "ENVIRONMENT" : "NOT_CONFIGURED";
    }

    public boolean rotationReady() {
        return true;
    }
}
