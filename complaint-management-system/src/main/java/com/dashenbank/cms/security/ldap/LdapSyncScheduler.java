package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.LdapSyncStatus;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.LdapSyncStatusRepository;
import com.dashenbank.cms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LdapSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LdapSyncScheduler.class);

    private final LdapProperties properties;
    private final DirectoryOperations directory;
    private final AdUserSyncService syncService;
    private final UserRepository userRepository;
    private final LdapSyncStatusRepository statusRepository;
    private final Clock clock;
    @Autowired(required = false)
    private com.dashenbank.cms.service.SecurityAuditService securityAuditService;

    @Autowired
    public LdapSyncScheduler(LdapProperties properties, DirectoryOperations directory, AdUserSyncService syncService,
            UserRepository userRepository, LdapSyncStatusRepository statusRepository) {
        this(properties, directory, syncService, userRepository, statusRepository, Clock.systemDefaultZone());
    }

    LdapSyncScheduler(LdapProperties properties, DirectoryOperations directory, AdUserSyncService syncService,
            UserRepository userRepository, LdapSyncStatusRepository statusRepository, Clock clock) {
        this.properties = properties;
        this.directory = directory;
        this.syncService = syncService;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${ldap.sync.cron:0 0 * * * *}")
    public void scheduledSync() {
        if (!properties.isEnabled() || !properties.getSync().isEnabled()) {
            return;
        }
        runSync();
    }

    public SyncRunResult runSync() {
        LdapSyncStatus status = loadStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        status.setLastStartedAt(now);
        status.setUpdatedAt(now);
        DirectoryHealth health = directory.health();
        status.setDirectoryReachable(health.reachable());
        status.setLastHealthCheckAt(now);
        if (!health.reachable()) {
            status.setLastResult("UNAVAILABLE");
            status.setLastError(truncate(health.detail()));
            status.setLastFinishedAt(now);
            statusRepository.save(status);
            return new SyncRunResult(0, 0, health.detail());
        }

        int synced = 0;
        int failed = 0;
        try {
            Map<String, AdUserProfile> byName = collectProfilesToSync();
            SyncCounts counts = syncCollectedProfiles(byName);
            synced = counts.synced();
            failed = counts.failed();
            status.setLastResult("SUCCESS");
            status.setLastError(null);
            status.setLastSuccessAt(now);
        } catch (RuntimeException e) {
            status.setLastResult("FAILED");
            status.setLastError(truncate(e.getClass().getSimpleName()));
            failed++;
        }
        status.setLastFinishedAt(LocalDateTime.now(clock));
        status.setUpdatedAt(status.getLastFinishedAt());
        status.setUsersSynced(synced);
        status.setUsersFailed(failed);
        statusRepository.save(status);
        if (securityAuditService != null) {
            securityAuditService.log("system", com.dashenbank.cms.model.SecurityAuditEvent.LDAP_SYNC, "");
        }
        return new SyncRunResult(synced, failed, status.getLastError());
    }

    private Map<String, AdUserProfile> collectProfilesToSync() {
        Map<String, AdUserProfile> byName = new LinkedHashMap<>();
        if (properties.getSync().isDiscovery()) {
            for (AdUserProfile profile : directory.searchDirectoryUsers(properties.getSync().getMaxResults())) {
                byName.put(profile.samAccountName().toLowerCase(), profile);
            }
        }
        for (User user : userRepository.findByAuthSource(AuthSource.AD)) {
            directory.findBySamAccountName(user.getUsername())
                    .ifPresent(profile -> byName.put(profile.samAccountName().toLowerCase(), profile));
        }
        return byName;
    }

    private SyncCounts syncCollectedProfiles(Map<String, AdUserProfile> byName) {
        int synced = 0;
        int failed = 0;
        for (AdUserProfile profile : byName.values()) {
            SyncOutcome outcome = syncOneProfile(profile);
            if (outcome == SyncOutcome.SYNCED) {
                synced++;
            } else if (outcome == SyncOutcome.FAILED) {
                failed++;
            }
        }
        return new SyncCounts(synced, failed);
    }

    private SyncOutcome syncOneProfile(AdUserProfile profile) {
        try {
            return syncService.syncIdentity(profile) ? SyncOutcome.SYNCED : SyncOutcome.SKIPPED;
        } catch (RuntimeException e) {
            log.warn("LDAP sync skipped a user: {}", e.getClass().getSimpleName());
            return SyncOutcome.FAILED;
        }
    }

    private enum SyncOutcome {
        SYNCED, SKIPPED, FAILED
    }

    private record SyncCounts(int synced, int failed) {
    }

    public DirectoryHealth refreshHealth() {
        DirectoryHealth health = directory.health();
        LdapSyncStatus status = loadStatus();
        status.setDirectoryReachable(health.reachable());
        status.setLastHealthCheckAt(LocalDateTime.now(clock));
        if (!health.reachable()) {
            status.setLastError(truncate(health.detail()));
        }
        status.setUpdatedAt(status.getLastHealthCheckAt());
        statusRepository.save(status);
        return health;
    }

    public LdapSyncStatus loadStatus() {
        return statusRepository.findById(LdapSyncStatus.SINGLETON_ID).orElseGet(() -> {
            LdapSyncStatus created = new LdapSyncStatus();
            created.setId(LdapSyncStatus.SINGLETON_ID);
            created.setLastResult("NEVER_RUN");
            return statusRepository.save(created);
        });
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
