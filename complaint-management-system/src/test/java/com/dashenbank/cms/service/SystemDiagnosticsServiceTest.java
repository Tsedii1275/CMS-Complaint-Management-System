package com.dashenbank.cms.service;

import com.dashenbank.cms.integration.corebanking.CbsCustomerProfileRepository;
import com.dashenbank.cms.model.LdapSyncStatus;
import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.SecurityAuditLog;
import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.NotificationRecordRepository;
import com.dashenbank.cms.notification.NotificationStatus;
import com.dashenbank.cms.repository.SecurityAuditLogRepository;
import com.dashenbank.cms.security.ldap.DirectoryHealth;
import com.dashenbank.cms.security.ldap.LdapProperties;
import com.dashenbank.cms.security.ldap.LdapSyncScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemDiagnosticsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private LdapSyncScheduler ldapSyncScheduler;
    @Mock
    private CbsCustomerProfileRepository cbsRepository;
    @Mock
    private NotificationRecordRepository notificationRepository;
    @Mock
    private SecurityAuditLogRepository securityAuditLogRepository;

    private NotificationProperties notificationProperties;
    private LdapProperties ldapProperties;
    private SystemDiagnosticsService service;

    @BeforeEach
    void setUp() {
        notificationProperties = new NotificationProperties();
        ldapProperties = new LdapProperties();
        service = new SystemDiagnosticsService(jdbcTemplate, ldapProperties, ldapSyncScheduler, cbsRepository,
                notificationProperties, notificationRepository, securityAuditLogRepository, null);
    }

    @Test
    void overviewReportsLiveDatabaseAndOmitsSecrets() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        ldapProperties.setEnabled(false);
        when(cbsRepository.isConfigured()).thenReturn(false);
        notificationProperties.setEnabled(false);
        when(notificationRepository.countByChannelAndStatusIn(any(), any())).thenReturn(0L);

        Map<String, Object> overview = service.overview();
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) overview.get("database");
        assertEquals("UP", database.get("status"));
        assertFalse(overview.toString().toLowerCase().contains("password"));
    }

    @Test
    void integrationsShowLdapFailureAndSmsHostWithoutCredentials() {
        ldapProperties.setEnabled(true);
        when(ldapSyncScheduler.refreshHealth())
                .thenReturn(new DirectoryHealth(false, "ldaps://dc", "password=secret refused"));
        when(ldapSyncScheduler.loadStatus()).thenReturn(new LdapSyncStatus());
        when(cbsRepository.isConfigured()).thenReturn(true);
        when(cbsRepository.probe()).thenReturn("up");
        notificationProperties.setEnabled(true);
        notificationProperties.getEmail().setEnabled(true);
        notificationProperties.getEmail().setProvider("smtp");
        notificationProperties.getSms().setEnabled(true);
        notificationProperties.getSms().getDatapower()
                .setTokenUrl("https://datapower.example:9443/token");
        notificationProperties.getSms().getDatapower()
                .setSendUrl("https://datapower.example:9443/sms");
        notificationProperties.getSms().getDatapower().setClientSecret("do-not-leak");
        when(notificationRepository.findFirstByChannelAndStatusOrderBySentAtDesc(any(), eq(NotificationStatus.SENT)))
                .thenReturn(Optional.empty());
        when(notificationRepository.findFirstByChannelAndStatusOrderByIdDesc(any(), eq(NotificationStatus.FAILED)))
                .thenReturn(Optional.empty());
        when(notificationRepository.countByChannelAndStatusIn(eq(NotificationChannel.SMS), any())).thenReturn(2L);
        when(notificationRepository.countByChannelAndStatusIn(eq(NotificationChannel.EMAIL), any())).thenReturn(0L);

        Map<String, Object> body = service.integrations();
        String rendered = body.toString();
        assertTrue(rendered.contains("datapower.example"));
        assertFalse(rendered.contains("do-not-leak"));
        assertFalse(rendered.contains("password=secret"));
        @SuppressWarnings("unchecked")
        Map<String, Object> queues = (Map<String, Object>) body.get("queues");
        assertEquals(2L, queues.get("smsPending"));
    }

    @Test
    void securityLogSearchIsReadOnlyAndFiltersByUser() {
        SecurityAuditLog roleChange = SecurityAuditLog.builder()
                .id(1L)
                .username("admin")
                .eventType(SecurityAuditEvent.ROLE_CHANGED)
                .createdAt(LocalDateTime.now())
                .ipAddress("10.1.1.1")
                .build();
        SecurityAuditLog login = SecurityAuditLog.builder()
                .id(2L)
                .username("other")
                .eventType(SecurityAuditEvent.SUCCESSFUL_LOGIN)
                .createdAt(LocalDateTime.now())
                .ipAddress("10.1.1.2")
                .build();
        when(securityAuditLogRepository.findTop500ByOrderByCreatedAtDesc()).thenReturn(List.of(roleChange, login));

        List<Map<String, Object>> rows = service.securityLogs("admin", "ROLE_CHANGED");
        assertEquals(1, rows.size());
        assertEquals("ROLE_CHANGED", rows.get(0).get("eventType"));
    }

    @Test
    void safeDetailDropsCredentialText() {
        assertEquals("Check failed. Credential details were omitted.",
                SystemDiagnosticsService.safeDetail("jdbc:oracle:thin:user/password@host"));
    }
}
