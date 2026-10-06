package com.dashenbank.cms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "stage_sla_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageSlaEvent {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_APPROACHING = "APPROACHING";
    public static final String STATUS_BREACHED = "BREACHED";
    public static final String STATUS_MET = "MET";

    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", length = 100)
    private String complaintId;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "task_id", length = 100, nullable = false, unique = true)
    private String taskId;

    @Column(name = "task_definition_key", length = 100)
    private String taskDefinitionKey;

    @Column(name = "canonical_stage", length = 80, nullable = false)
    private String canonicalStage;

    @Column(name = "assigned_user", length = 100)
    private String assignedUser;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "allowed_minutes")
    private Integer allowedMinutes;

    @Column(name = "elapsed_business_minutes")
    private Integer elapsedBusinessMinutes;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = STATUS_IN_PROGRESS;

    @Column(name = "breached_at")
    private LocalDateTime breachedAt;

    @Column(name = "breach_recorded", nullable = false)
    @Builder.Default
    private Boolean breachRecorded = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = STATUS_IN_PROGRESS;
        }
        if (breachRecorded == null) {
            breachRecorded = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(SYSTEM_ZONE);
    }

    public boolean isOpen() {
        return completedAt == null
                && (STATUS_IN_PROGRESS.equalsIgnoreCase(status) || STATUS_APPROACHING.equalsIgnoreCase(status)
                        || STATUS_BREACHED.equalsIgnoreCase(status));
    }
}
