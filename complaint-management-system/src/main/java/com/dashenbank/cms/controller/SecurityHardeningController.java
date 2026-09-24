package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.SecurityAuditLog;
import com.dashenbank.cms.repository.SecurityAuditLogRepository;
import com.dashenbank.cms.security.SecurityHardeningService;
import com.dashenbank.cms.security.SensitiveDataMasker;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/security")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class SecurityHardeningController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<SecurityAuditEvent> AUTH_EVENTS = EnumSet.of(
            SecurityAuditEvent.SUCCESSFUL_LOGIN,
            SecurityAuditEvent.FAILED_LOGIN,
            SecurityAuditEvent.LOGOUT,
            SecurityAuditEvent.ACCOUNT_LOCKED);

    private final SecurityHardeningService securityHardeningService;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    public SecurityHardeningController(SecurityHardeningService securityHardeningService,
            SecurityAuditLogRepository securityAuditLogRepository) {
        this.securityHardeningService = securityHardeningService;
        this.securityAuditLogRepository = securityAuditLogRepository;
    }

    @GetMapping("/system-info")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        return ResponseEntity.ok(securityHardeningService.systemInfo());
    }

    @GetMapping("/auth-events")
    public ResponseEntity<List<Map<String, Object>>> authEvents() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SecurityAuditLog event : securityAuditLogRepository
                .findTop100ByEventTypeInOrderByCreatedAtDesc(new ArrayList<>(AUTH_EVENTS))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", event.getUsername());
            row.put("loginTime", event.getCreatedAt() == null ? "" : event.getCreatedAt().format(TS));
            row.put("success", event.getEventType() == SecurityAuditEvent.SUCCESSFUL_LOGIN
                    || event.getEventType() == SecurityAuditEvent.LOGOUT);
            row.put("outcome", event.getEventType().name());
            row.put("sourceIp", event.getIpAddress() == null ? "" : event.getIpAddress());
            rows.add(row);
        }
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/rbac")
    public ResponseEntity<List<Map<String, Object>>> rbac() {
        return ResponseEntity.ok(securityHardeningService.rbac());
    }

    @GetMapping("/encryption")
    public ResponseEntity<Map<String, Object>> encryption() {
        return ResponseEntity.ok(securityHardeningService.encryption());
    }

    @GetMapping("/http-headers")
    public ResponseEntity<Map<String, Object>> httpHeaders() {
        return ResponseEntity.ok(securityHardeningService.httpHeaders());
    }

    @GetMapping("/http-security")
    public ResponseEntity<Map<String, Object>> httpSecurity() {
        return ResponseEntity.ok(securityHardeningService.httpSecurity());
    }

    @GetMapping("/validation")
    public ResponseEntity<List<Map<String, Object>>> validation() {
        return ResponseEntity.ok(securityHardeningService.validation());
    }

    @GetMapping("/audit-summary")
    public ResponseEntity<List<Map<String, Object>>> auditSummary() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SecurityAuditLogRepository.EventSummary summary : securityAuditLogRepository.summarizeByEvent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("eventType", summary.getEventType().name());
            row.put("count", summary.getEventCount());
            row.put("lastOccurrence", summary.getLastOccurrence() == null ? "" : summary.getLastOccurrence().format(TS));
            rows.add(row);
        }
        for (SecurityAuditEvent expected : SecurityAuditEvent.values()) {
            boolean present = rows.stream().anyMatch(row -> expected.name().equals(row.get("eventType")));
            if (!present) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("eventType", expected.name());
                row.put("count", 0);
                row.put("lastOccurrence", "");
                rows.add(row);
            }
        }
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/active-sessions")
    public ResponseEntity<List<Map<String, Object>>> activeSessions() {
        return ResponseEntity.ok(securityHardeningService.activeSessions());
    }

    @GetMapping("/token-config")
    public ResponseEntity<Map<String, Object>> tokenConfig() {
        return ResponseEntity.ok(securityHardeningService.tokenConfig());
    }

    @GetMapping("/rate-limit-status")
    public ResponseEntity<List<Map<String, Object>>> rateLimitStatus() {
        return ResponseEntity.ok(securityHardeningService.rateLimits());
    }

    @GetMapping("/key-management")
    public ResponseEntity<Map<String, Object>> keyManagement() {
        return ResponseEntity.ok(securityHardeningService.keyManagement());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInfo", securityHardeningService.systemInfo());
        body.put("authentication", securityHardeningService.authentication());
        body.put("rbac", securityHardeningService.rbac());
        body.put("encryption", securityHardeningService.encryption());
        body.put("activeSessions", securityHardeningService.activeSessions());
        body.put("httpHeaders", securityHardeningService.httpHeaders());
        body.put("httpSecurity", securityHardeningService.httpSecurity());
        body.put("tokenConfig", securityHardeningService.tokenConfig());
        body.put("rateLimits", securityHardeningService.rateLimits());
        body.put("keyManagement", securityHardeningService.keyManagement());
        body.put("validation", securityHardeningService.validation());
        body.put("maskingExamples", Map.of(
                "phone", SensitiveDataMasker.phone("0911223344"),
                "email", SensitiveDataMasker.email("customer@email.com")));
        return ResponseEntity.ok(body);
    }
}
