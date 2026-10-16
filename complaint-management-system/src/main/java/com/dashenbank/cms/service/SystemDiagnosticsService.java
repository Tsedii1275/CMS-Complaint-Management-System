package com.dashenbank.cms.service;

import com.dashenbank.cms.integration.corebanking.CbsCustomerProfileRepository;
import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.SecurityAuditLog;
import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.NotificationRecord;
import com.dashenbank.cms.notification.NotificationRecordRepository;
import com.dashenbank.cms.notification.NotificationStatus;
import com.dashenbank.cms.repository.SecurityAuditLogRepository;
import com.dashenbank.cms.security.ldap.DirectoryHealth;
import com.dashenbank.cms.security.ldap.LdapProperties;
import com.dashenbank.cms.security.ldap.LdapSyncScheduler;
import org.flowable.engine.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Live system checks for the System Administrator dashboard. Responses name
 * status and a short safe detail. They do not include passwords, tokens, or
 * connection strings.
 */
@Service
public class SystemDiagnosticsService {

    private final JdbcTemplate jdbcTemplate;
    private final LdapProperties ldapProperties;
    private final LdapSyncScheduler ldapSyncScheduler;
    private final CbsCustomerProfileRepository cbsRepository;
    private final NotificationProperties notificationProperties;
    private final NotificationRecordRepository notificationRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;
    private final HistoryService historyService;

    public SystemDiagnosticsService(JdbcTemplate jdbcTemplate,
            LdapProperties ldapProperties, LdapSyncScheduler ldapSyncScheduler,
            CbsCustomerProfileRepository cbsRepository, NotificationProperties notificationProperties,
            NotificationRecordRepository notificationRepository, SecurityAuditLogRepository securityAuditLogRepository,
            @Autowired(required = false) HistoryService historyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.ldapProperties = ldapProperties;
        this.ldapSyncScheduler = ldapSyncScheduler;
        this.cbsRepository = cbsRepository;
        this.notificationProperties = notificationProperties;
        this.notificationRepository = notificationRepository;
        this.securityAuditLogRepository = securityAuditLogRepository;
        this.historyService = historyService;
    }

    public Map<String, Object> overview() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("checkedAt", LocalDateTime.now().toString());
        body.put("application", component("application", "Application", "UP", "CMS API is responding"));
        body.put("database", database());
        body.put("integrations", integrations().get("components"));
        return body;
    }

    public Map<String, Object> integrations() {
        List<Map<String, Object>> components = new ArrayList<>();
        components.add(ldap());
        components.add(cbs());
        components.add(email());
        components.add(sms());
        components.add(flowable());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("checkedAt", LocalDateTime.now().toString());
        body.put("components", components);
        body.put("queues", queues());
        return body;
    }

    public List<Map<String, Object>> securityLogs(String query, String eventType) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        SecurityAuditEvent wanted = parseEvent(eventType);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SecurityAuditLog log : securityAuditLogRepository.findTop500ByOrderByCreatedAtDesc()) {
            if (wanted != null && log.getEventType() != wanted) {
                continue;
            }
            if (!needle.isEmpty() && !matches(log, needle)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("username", log.getUsername());
            row.put("eventType", log.getEventType() == null ? "" : log.getEventType().name());
            row.put("createdAt", log.getCreatedAt() == null ? "" : log.getCreatedAt().toString());
            row.put("ipAddress", log.getIpAddress() == null ? "" : log.getIpAddress());
            rows.add(row);
            if (rows.size() >= 200) {
                break;
            }
        }
        return rows;
    }

    private Map<String, Object> database() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (one != null && one == 1) {
                return component("database", "Application database", "UP", "Database accepted SELECT 1");
            }
            return component("database", "Application database", "DOWN", "Database returned an unexpected result");
        } catch (DataAccessException e) {
            return component("database", "Application database", "DOWN", "Database connection failed");
        }
    }

    private Map<String, Object> ldap() {
        if (!ldapProperties.isEnabled()) {
            return component("ldap", "AD / LDAP", "DISABLED", "LDAP is turned off");
        }
        DirectoryHealth health = ldapSyncScheduler.refreshHealth();
        var sync = ldapSyncScheduler.loadStatus();
        String status = health.reachable() ? "UP" : "DOWN";
        String detail = health.reachable() ? "Directory bind succeeded" : safeDetail(health.detail());
        Map<String, Object> row = component("ldap", "AD / LDAP", status, detail);
        row.put("lastSuccessAt", sync.getLastSuccessAt());
        row.put("lastCheckedAt", sync.getLastHealthCheckAt());
        return row;
    }

    private Map<String, Object> cbs() {
        if (!cbsRepository.isConfigured()) {
            return component("cbs", "Core Banking (CBS)", "DISABLED", "CBS datasource is not configured");
        }
        String probe = cbsRepository.probe();
        if ("up".equals(probe)) {
            return component("cbs", "Core Banking (CBS)", "UP", "Oracle accepted SELECT 1 FROM DUAL");
        }
        return component("cbs", "Core Banking (CBS)", "DOWN", "CBS connection failed");
    }

    private Map<String, Object> email() {
        boolean enabled = notificationProperties.isEnabled() && notificationProperties.getEmail().isEnabled();
        if (!enabled) {
            return component("email", "Email", "DISABLED", "Email notifications are turned off");
        }
        String provider = notificationProperties.getEmail().getProvider();
        Map<String, Object> row = component("email", "Email", "UP",
                "Provider " + (provider == null || provider.isBlank() ? "unspecified" : provider));
        notificationRepository.findFirstByChannelAndStatusOrderBySentAtDesc(NotificationChannel.EMAIL,
                NotificationStatus.SENT).ifPresent(sent -> row.put("lastSuccessAt", sent.getSentAt()));
        latestFailure(NotificationChannel.EMAIL, row);
        return row;
    }

    private Map<String, Object> sms() {
        boolean enabled = notificationProperties.isEnabled() && notificationProperties.getSms().isEnabled();
        if (!enabled) {
            return component("sms", "SMS / DataPower", "DISABLED", "SMS notifications are turned off");
        }
        var datapower = notificationProperties.getSms().getDatapower();
        boolean addressesPresent = hasText(datapower.getTokenUrl()) && hasText(datapower.getSendUrl());
        String status = addressesPresent ? "UP" : "DOWN";
        String detail = addressesPresent
                ? "DataPower endpoint configured at " + hostOnly(datapower.getSendUrl())
                : "DataPower send URL is not configured";
        Map<String, Object> row = component("sms", "SMS / DataPower", status, detail);
        notificationRepository.findFirstByChannelAndStatusOrderBySentAtDesc(NotificationChannel.SMS,
                NotificationStatus.SENT).ifPresent(sent -> row.put("lastSuccessAt", sent.getSentAt()));
        latestFailure(NotificationChannel.SMS, row);
        return row;
    }

    private Map<String, Object> flowable() {
        if (historyService == null) {
            return component("flowable", "Flowable", "DOWN", "Workflow engine is not available");
        }
        try {
            historyService.createHistoricProcessInstanceQuery().count();
            return component("flowable", "Flowable", "UP", "Workflow history query succeeded");
        } catch (RuntimeException e) {
            return component("flowable", "Flowable", "DOWN", "Workflow engine query failed");
        }
    }

    private Map<String, Object> queues() {
        Map<String, Object> queues = new LinkedHashMap<>();
        queues.put("smsPending", count(NotificationChannel.SMS, NotificationStatus.PENDING, NotificationStatus.RETRY,
                NotificationStatus.SENDING));
        queues.put("smsFailed", count(NotificationChannel.SMS, NotificationStatus.FAILED));
        return queues;
    }

    private long count(NotificationChannel channel, NotificationStatus... statuses) {
        return notificationRepository.countByChannelAndStatusIn(channel, List.of(statuses));
    }

    private void latestFailure(NotificationChannel channel, Map<String, Object> row) {
        notificationRepository.findFirstByChannelAndStatusOrderByIdDesc(channel, NotificationStatus.FAILED)
                .ifPresent(failed -> {
                    row.put("lastErrorAt", failed.getLastAttemptAt());
                    row.put("lastError", safeDetail(failed.getFailureReason()));
                });
    }

    private static Map<String, Object> component(String id, String name, String status, String detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("status", status);
        row.put("detail", detail == null ? "" : detail);
        return row;
    }

    private static boolean matches(SecurityAuditLog log, String needle) {
        String username = log.getUsername() == null ? "" : log.getUsername().toLowerCase(Locale.ROOT);
        String ip = log.getIpAddress() == null ? "" : log.getIpAddress().toLowerCase(Locale.ROOT);
        String event = log.getEventType() == null ? "" : log.getEventType().name().toLowerCase(Locale.ROOT);
        return username.contains(needle) || ip.contains(needle) || event.contains(needle);
    }

    private static SecurityAuditEvent parseEvent(String eventType) {
        if (eventType == null || eventType.isBlank() || "ALL".equalsIgnoreCase(eventType)) {
            return null;
        }
        try {
            return SecurityAuditEvent.valueOf(eventType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static String safeDetail(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("password") || lower.contains("secret") || lower.contains("jdbc:")
                || lower.contains("client_secret") || lower.contains("authorization")) {
            return "Check failed. Credential details were omitted.";
        }
        String trimmed = raw.replaceAll("[\\r\\n\\t]", " ").trim();
        return trimmed.length() > 180 ? trimmed.substring(0, 180) : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String hostOnly(String url) {
        try {
            String host = URI.create(url.trim()).getHost();
            return host == null || host.isBlank() ? "configured host" : host;
        } catch (IllegalArgumentException e) {
            return "configured host";
        }
    }
}
