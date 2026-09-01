package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_time_tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTimeTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "complaint_id", length = 100)
    private String complaintId;

    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "task_definition_key", length = 100)
    private String taskDefinitionKey;

    @Column(name = "task_name", length = 255)
    private String taskName;

    @Column(name = "lane_name", length = 50)
    private String laneName;

    @Column(name = "assigned_user", length = 100)
    private String assignedUser;

    @Column(name = "assigned_role", length = 50)
    private String assignedRole;

    @Column(name = "assigned_department", length = 100)
    private String assignedDepartment;

    @Column(name = "assigned_branch", length = 100)
    private String assignedBranch;

    @Column(name = "assigned_district", length = 100)
    private String assignedDistrict;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "is_claimed")
    @Builder.Default
    private Boolean isClaimed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Response SLA Metrics (Assignment -> Claim)
    @Column(name = "response_time_minutes")
    private Long responseTimeMinutes;

    @Column(name = "response_sla_target_minutes")
    private Integer responseSlaTargetMinutes;

    @Column(name = "response_sla_status", length = 30)
    private String responseSlaStatus;

    @Column(name = "response_breach_duration_minutes")
    private Long responseBreachDurationMinutes;

    // Resolution SLA Metrics (Claim -> Completion)
    @Column(name = "resolution_time_minutes")
    private Long resolutionTimeMinutes;

    @Column(name = "resolution_sla_target_minutes")
    private Integer resolutionSlaTargetMinutes;

    @Column(name = "resolution_sla_status", length = 30)
    private String resolutionSlaStatus;

    @Column(name = "resolution_breach_duration_minutes")
    private Long resolutionBreachDurationMinutes;

    @Column(name = "duration_minutes")
    private Long durationMinutes;

    @Column(name = "duration_hours")
    private Double durationHours;
}
