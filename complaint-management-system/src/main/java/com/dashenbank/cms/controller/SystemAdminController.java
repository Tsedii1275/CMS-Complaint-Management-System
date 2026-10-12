package com.dashenbank.cms.controller;

import com.dashenbank.cms.service.SystemDiagnosticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system-admin")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class SystemAdminController {

    private final SystemDiagnosticsService diagnosticsService;

    public SystemAdminController(SystemDiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(diagnosticsService.overview());
    }

    @GetMapping("/integrations")
    public ResponseEntity<Map<String, Object>> integrations() {
        return ResponseEntity.ok(diagnosticsService.integrations());
    }

    @GetMapping("/security-logs")
    public ResponseEntity<List<Map<String, Object>>> securityLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String eventType) {
        return ResponseEntity.ok(diagnosticsService.securityLogs(q, eventType));
    }
}
