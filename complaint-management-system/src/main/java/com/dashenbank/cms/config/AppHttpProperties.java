package com.dashenbank.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Browser-facing origin for email/SMS links and CORS. TLS termination stays at
 * F5/IIS/Nginx; this is not a Spring SSL keystore helper.
 */
@Component
public class AppHttpProperties {

    private final String publicBaseUrl;
    private final String[] corsAllowedOrigins;

    public AppHttpProperties(
            @Value("${app.public-base-url:http://localhost:3000}") String publicBaseUrl,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String corsAllowedOrigins) {
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
        this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public String pageUrl(String path) {
        String normalized = path == null ? "" : path;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return publicBaseUrl + normalized;
    }

    public String[] corsAllowedOrigins() {
        return corsAllowedOrigins.clone();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:3000";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
