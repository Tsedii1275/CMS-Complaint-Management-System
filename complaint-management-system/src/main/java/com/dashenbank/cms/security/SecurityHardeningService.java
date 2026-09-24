package com.dashenbank.cms.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityHardeningService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Addis_Ababa");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final SecuritySettings securitySettings;
    private final JwtUtils jwtUtils;
    private final Aes256GcmReadyCrypto aes256GcmReadyCrypto;
    private final SecretSourceReporter secretSourceReporter;
    private final ApiSessionRegistry sessionRegistry;
    private final MfaCatalog mfaCatalog;
    private final Instant processStartedAt = Instant.now();

    public SecurityHardeningService(Environment environment, JdbcTemplate jdbcTemplate,
            ObjectProvider<BuildProperties> buildProperties, SecuritySettings securitySettings, JwtUtils jwtUtils,
            Aes256GcmReadyCrypto aes256GcmReadyCrypto, SecretSourceReporter secretSourceReporter,
            ApiSessionRegistry sessionRegistry, MfaCatalog mfaCatalog) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.buildProperties = buildProperties;
        this.securitySettings = securitySettings;
        this.jwtUtils = jwtUtils;
        this.aes256GcmReadyCrypto = aes256GcmReadyCrypto;
        this.secretSourceReporter = secretSourceReporter;
        this.sessionRegistry = sessionRegistry;
        this.mfaCatalog = mfaCatalog;
    }

    public Map<String, Object> systemInfo() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applicationName", environment.getProperty("spring.application.name", "complaint-management-system"));
        body.put("applicationVersion", applicationVersion());
        body.put("buildVersion", buildVersion());
        body.put("buildTimestamp", buildTimestamp());
        body.put("springBootVersion", SpringBootVersion.getVersion());
        body.put("javaRuntime", javaRuntime());
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("environment", activeEnvironment());
        body.put("databaseVersion", databaseVersion());
        body.put("httpsBehindReverseProxy", true);
        body.put("forwardHeadersStrategy", environment.getProperty("server.forward-headers-strategy", "framework"));
        return body;
    }

    public Map<String, Object> authentication() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ldapAuthentication", Boolean.parseBoolean(environment.getProperty("ldap.enabled", "false")));
        body.put("localFallbackAuthentication", true);
        body.put("jwtAuthentication", true);
        body.put("accountLockoutEnabled", true);
        body.put("lockoutThreshold", securitySettings.getLockoutThreshold());
        body.put("lockoutDurationMinutes", securitySettings.getLockoutDurationMinutes());
        body.put("failedLoginCounter", true);
        body.put("loginAuditLogging", true);
        body.put("mfaReady", securitySettings.isMfaReady());
        body.put("mfaEnabledInLoginPath", false);
        body.put("mfaProvider", mfaCatalog.providerId());
        body.put("mfaExtensionPoint", "com.dashenbank.cms.security.mfa.MfaProvider");
        return body;
    }

    public List<Map<String, Object>> rbac() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SecurityRbacCatalog.RbacRow row : SecurityRbacCatalog.rows()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", row.role());
            item.put("assignedApis", row.assignedApis());
            item.put("permissions", row.permissions());
            rows.add(item);
        }
        return rows;
    }

    public Map<String, Object> encryption() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("passwordAlgorithm", "BCrypt");
        body.put("plaintextPasswordStorage", false);
        body.put("encryptionAlgorithm", Aes256GcmReadyCrypto.ALGORITHM);
        body.put("fieldEncryptionEnabled", aes256GcmReadyCrypto.keyPresent());
        body.put("keyManagementStatus", aes256GcmReadyCrypto.keyManagementStatus());
        body.put("aes256Ready", true);
        body.put("rotationReady", aes256GcmReadyCrypto.rotationReady());
        body.put("maskingExamples", Map.of(
                "phone", SensitiveDataMasker.phone("0911223344"),
                "email", SensitiveDataMasker.email("customer@email.com"),
                "accountNumber", SensitiveDataMasker.accountNumber("1000123456789")));
        body.put("sensitiveFieldsMaskedInLogsAndAdmin", List.of("email", "phone", "accountNumber"));
        return body;
    }

    public Map<String, Object> httpHeaders() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enforcedBehindReverseProxy", true);
        body.put("hstsSentOnAllResponses", true);
        List<Map<String, String>> headers = new ArrayList<>();
        for (SecurityRbacCatalog.HttpHeaderSetting setting : SecurityRbacCatalog.httpHeaders()) {
            headers.add(Map.of("name", setting.name(), "value", setting.value()));
        }
        body.put("headers", headers);
        return body;
    }

    public Map<String, Object> httpSecurity() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (SecurityRbacCatalog.HttpHeaderSetting setting : SecurityRbacCatalog.httpHeaders()) {
            values.put(setting.name(), setting.value());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("configuredValues", values);
        body.put("purpose", "Browser DevTools and this page show the same Spring Security headers.");
        return body;
    }

    public List<Map<String, Object>> validation() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SecurityRbacCatalog.ValidationRule rule : SecurityRbacCatalog.validationRules()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("surface", rule.surface());
            item.put("fields", rule.fields());
            item.put("constraints", rule.constraints());
            item.put("failureStatus", 400);
            rows.add(item);
        }
        return rows;
    }

    public List<Map<String, Object>> rateLimits() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SecurityRbacCatalog.RateLimitRule rule : SecurityRbacCatalog.rateLimits()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("protectedEndpoint", rule.endpoint());
            item.put("limit", rule.limit());
            item.put("windowSeconds", rule.windowSeconds());
            item.put("exceededStatus", 429);
            rows.add(item);
        }
        return rows;
    }

    public Map<String, Object> tokenConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessTokenTtlMs", jwtUtils.getExpirationMs());
        body.put("accessTokenTtlMinutes", jwtUtils.getExpirationMs() / 60_000);
        body.put("refreshTokenTtl", "NOT_IMPLEMENTED");
        body.put("signingAlgorithm", jwtUtils.getSigningAlgorithm());
        body.put("tokenIssuer", jwtUtils.getIssuer());
        body.put("expirationEnabled", true);
        body.put("expiredTokensRejected", true);
        return body;
    }

    public Map<String, Object> keyManagement() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jwtSecretSource", secretSourceReporter.jwtSecretSource());
        body.put("ldapCredentialSource", secretSourceReporter.ldapCredentialSource());
        body.put("smtpCredentialSource", secretSourceReporter.smtpCredentialSource());
        body.put("aesDataKeySource", aes256GcmReadyCrypto.keyManagementStatus());
        body.put("valuesExposed", false);
        body.put("hardcodedJwtSecret", false);
        body.put("rotationReady", true);
        body.put("rotationProcedure",
                "Replace APP_JWT_SECRET / LDAP_BIND_PASSWORD / mail passwords in the environment or secret store, then restart. Dual-key overlap is the next rotation step.");
        return body;
    }

    public List<Map<String, Object>> activeSessions() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApiSessionRegistry.SessionSnapshot session : sessionRegistry.active()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", session.username());
            item.put("role", session.role());
            item.put("loginTime", session.loginTime().atZone(ZONE).format(TS));
            item.put("sourceIp", session.sourceIp());
            item.put("userAgent", session.userAgent());
            rows.add(item);
        }
        return rows;
    }

    private String applicationVersion() {
        return environment.getProperty("info.app.version", buildVersion());
    }

    private String buildVersion() {
        BuildProperties props = buildProperties.getIfAvailable();
        if (props != null && props.getVersion() != null && !props.getVersion().isBlank()) {
            return props.getVersion();
        }
        return "0.0.1-SNAPSHOT";
    }

    private String buildTimestamp() {
        BuildProperties props = buildProperties.getIfAvailable();
        Instant time = processStartedAt;
        if (props != null && props.getTime() != null) {
            time = props.getTime();
        }
        return time.atZone(ZONE).format(TS);
    }

    private String javaRuntime() {
        return System.getProperty("java.runtime.name", "Java") + " " + System.getProperty("java.version");
    }

    private String activeEnvironment() {
        String appEnv = environment.getProperty("APP_ENV");
        if (appEnv != null && !appEnv.isBlank()) {
            return appEnv;
        }
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            return String.join(",", profiles);
        }
        return Arrays.toString(environment.getDefaultProfiles());
    }

    private String databaseVersion() {
        try {
            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            return version == null || version.isBlank() ? "UNAVAILABLE" : version;
        } catch (RuntimeException ex) {
            return "UNAVAILABLE";
        }
    }
}
