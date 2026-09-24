package com.dashenbank.cms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ldap_sync_status")
@Getter
@Setter
@NoArgsConstructor
public class LdapSyncStatus {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    private LocalDateTime lastStartedAt;
    private LocalDateTime lastFinishedAt;
    private LocalDateTime lastSuccessAt;
    private String lastResult;
    private int usersSynced;
    private int usersFailed;
    private String lastError;
    private boolean directoryReachable;
    private LocalDateTime lastHealthCheckAt;
    private LocalDateTime updatedAt;
}
