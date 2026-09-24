package com.dashenbank.cms.security;

import com.dashenbank.cms.security.mfa.DisabledMfaProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHardeningServiceTest {

    private Environment environment;
    private JdbcTemplate jdbcTemplate;
    private JwtUtils jwtUtils;
    private SecurityHardeningService service;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        jwtUtils = mock(JwtUtils.class);
        when(environment.getProperty("spring.application.name", "complaint-management-system"))
                .thenReturn("complaint-management-system");
        when(environment.getProperty(org.mockito.ArgumentMatchers.eq("info.app.version"),
                org.mockito.ArgumentMatchers.anyString())).thenReturn("0.0.1-SNAPSHOT");
        when(environment.getProperty("APP_ENV")).thenReturn("prod");
        when(environment.getProperty("server.forward-headers-strategy", "framework")).thenReturn("framework");
        when(environment.getProperty("APP_JWT_SECRET")).thenReturn("secret-from-env-not-returned");
        when(environment.getProperty("LDAP_BIND_PASSWORD")).thenReturn("ldap-from-env");
        when(environment.getProperty("GMAIL_SMTP_PASSWORD")).thenReturn("");
        when(environment.getProperty("SPRING_MAIL_PASSWORD")).thenReturn("");
        when(environment.getProperty("VAULT_TOKEN")).thenReturn(null);
        when(environment.getProperty("SPRING_CLOUD_VAULT_URI")).thenReturn(null);
        when(environment.getProperty("SPRING_CLOUD_CONFIG_URI")).thenReturn(null);
        when(environment.getProperty("AES_DATA_KEY")).thenReturn(null);
        when(jdbcTemplate.queryForObject("SELECT VERSION()", String.class)).thenReturn("8.0.36");
        when(jwtUtils.getExpirationMs()).thenReturn(900_000);
        when(jwtUtils.getSigningAlgorithm()).thenReturn("HS256");
        when(jwtUtils.getIssuer()).thenReturn("dashenbank-cms");

        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> buildProperties = mock(ObjectProvider.class);
        when(buildProperties.getIfAvailable()).thenReturn(null);

        SecuritySettings settings = new SecuritySettings();
        settings.setMfaReady(true);
        settings.setLockoutThreshold(6);
        settings.setLockoutDurationMinutes(30);
        settings.setJwtIssuer("dashenbank-cms");

        service = new SecurityHardeningService(environment, jdbcTemplate, buildProperties, settings, jwtUtils,
                new Aes256GcmReadyCrypto(environment), new SecretSourceReporter(environment),
                new ApiSessionRegistry(), new MfaCatalog(new DisabledMfaProvider()));
    }

    @Test
    void systemInfoExposesVersionsWithoutSecrets() {
        Map<String, Object> info = service.systemInfo();
        assertEquals("0.0.1-SNAPSHOT", info.get("applicationVersion"));
        assertEquals("prod", info.get("environment"));
        assertEquals("8.0.36", info.get("databaseVersion"));
        assertTrue(info.get("javaRuntime").toString().contains(System.getProperty("java.version")));
        assertFalse(info.values().stream().anyMatch(value -> String.valueOf(value).contains("secret-from-env")));
    }

    @Test
    void tokenConfigShowsTtlAlgorithmAndIssuer() {
        Map<String, Object> token = service.tokenConfig();
        assertEquals(900_000, token.get("accessTokenTtlMs"));
        assertEquals(15, token.get("accessTokenTtlMinutes"));
        assertEquals("HS256", token.get("signingAlgorithm"));
        assertEquals("dashenbank-cms", token.get("tokenIssuer"));
        assertEquals("NOT_IMPLEMENTED", token.get("refreshTokenTtl"));
        assertEquals(Boolean.TRUE, token.get("expiredTokensRejected"));
    }

    @Test
    void keyManagementReportsSourcesNotValues() {
        Map<String, Object> keys = service.keyManagement();
        assertEquals("ENVIRONMENT", keys.get("jwtSecretSource"));
        assertEquals("ENVIRONMENT", keys.get("ldapCredentialSource"));
        assertEquals("NOT_REQUIRED", keys.get("smtpCredentialSource"));
        assertEquals(Boolean.FALSE, keys.get("valuesExposed"));
        assertFalse(keys.values().stream().anyMatch(value -> String.valueOf(value).contains("secret-from-env")));
    }

    @Test
    void encryptionDocumentsBcryptAndAes256Readiness() {
        Map<String, Object> encryption = service.encryption();
        assertEquals("BCrypt", encryption.get("passwordAlgorithm"));
        assertEquals("AES-256-GCM", encryption.get("encryptionAlgorithm"));
        assertEquals("NOT_CONFIGURED", encryption.get("keyManagementStatus"));
        @SuppressWarnings("unchecked")
        Map<String, String> examples = (Map<String, String>) encryption.get("maskingExamples");
        assertEquals("0911****44", examples.get("phone"));
        assertEquals("cust****@email.com", examples.get("email"));
    }

    @Test
    void rateLimitsCoverLoginStartAndStatus() {
        List<Map<String, Object>> limits = service.rateLimits();
        assertTrue(limits.stream().anyMatch(row -> "POST /api/auth/login".equals(row.get("protectedEndpoint"))
                && Integer.valueOf(10).equals(row.get("limit"))));
        assertTrue(limits.stream().anyMatch(row -> "POST /api/complaints/start".equals(row.get("protectedEndpoint"))));
        assertTrue(limits.stream().anyMatch(row -> "GET /api/complaints/status/**".equals(row.get("protectedEndpoint"))
                && Integer.valueOf(429).equals(row.get("exceededStatus"))));
    }

    @Test
    void httpHeadersIncludeRequiredBankHeaders() {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> headers = (List<Map<String, String>>) service.httpHeaders().get("headers");
        assertTrue(headers.stream().anyMatch(row -> "Strict-Transport-Security".equals(row.get("name"))));
        assertTrue(headers.stream().anyMatch(row -> "Content-Security-Policy".equals(row.get("name"))));
        assertTrue(headers.stream().anyMatch(row -> "Permissions-Policy".equals(row.get("name"))));
    }

    @Test
    void activeSessionsNeverIncludeJwt() {
        ApiSessionRegistry registry = new ApiSessionRegistry();
        registry.opened("admin", "ROLE_ADMIN", "10.0.0.8", "Mozilla");
        assertEquals("admin", registry.active().get(0).username());
        assertFalse(registry.active().toString().toLowerCase().contains("bearer"));
        assertFalse(registry.active().toString().toLowerCase().contains("jwt"));
    }
}
