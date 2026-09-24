package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.LdapSyncStatus;
import com.dashenbank.cms.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ldap")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class LdapMaintenanceController {

    private final LdapProperties properties;
    private final DirectoryOperations directory;
    private final AdRoleMappingService roleMappingService;
    private final LdapSyncScheduler scheduler;
    private final UserRepository userRepository;

    public LdapMaintenanceController(LdapProperties properties, DirectoryOperations directory,
            AdRoleMappingService roleMappingService, LdapSyncScheduler scheduler, UserRepository userRepository) {
        this.properties = properties;
        this.directory = directory;
        this.roleMappingService = roleMappingService;
        this.scheduler = scheduler;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        DirectoryHealth health = scheduler.refreshHealth();
        LdapSyncStatus sync = scheduler.loadStatus();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ldapEnabled", properties.isEnabled());
        body.put("directoryConfigured", directory.configured());
        body.put("directoryReachable", health.reachable());
        body.put("url", properties.getUrl());
        body.put("fallbackUrl", properties.getFallbackUrl());
        body.put("baseDn", properties.getBaseDn());
        body.put("userSearchBase", properties.searchBase());
        body.put("sslPeerName", properties.getSslPeerName());
        body.put("rolePriority", properties.getRolePriority().name());
        body.put("adAuthenticationEnabled", properties.isEnabled() && directory.configured());
        body.put("lastHealthDetail", health.detail());
        body.put("lastHealthCheckAt", sync.getLastHealthCheckAt());
        body.put("lastSyncStartedAt", sync.getLastStartedAt());
        body.put("lastSyncFinishedAt", sync.getLastFinishedAt());
        body.put("lastSuccessfulSyncAt", sync.getLastSuccessAt());
        body.put("lastSyncResult", sync.getLastResult());
        body.put("usersSynced", sync.getUsersSynced());
        body.put("failedSynchronizations", sync.getUsersFailed());
        body.put("lastLdapError", sync.getLastError());
        body.put("adUsersInCms", userRepository.countByAuthSource(AuthSource.AD));
        body.put("titleMappings", roleMappingService.publishedTitleMappings());
        body.put("groupMappings", roleMappingService.publishedGroupMappings());
        body.put("scheduledSyncEnabled", properties.getSync().isEnabled());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        DirectoryHealth health = scheduler.refreshHealth();
        return ResponseEntity.ok(Map.of(
                "reachable", health.reachable(),
                "url", health.urlUsed(),
                "detail", health.detail() == null ? "" : health.detail()));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncNow() {
        SyncRunResult result = scheduler.runSync();
        return ResponseEntity.ok(Map.of(
                "usersSynced", result.synced(),
                "failedSynchronizations", result.failed(),
                "error", result.error() == null ? "" : result.error()));
    }
}
