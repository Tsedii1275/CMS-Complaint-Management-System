package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_sla_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSlaMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_instance_id", length = 100, unique = true)
    private String processInstanceId;

    @Column(name = "complaint_id", length = 100)
    private String complaintId;

    @Column(name = "complaint_category", length = 100)
    private String complaintCategory;

    @Column(name = "general_ticket_id", length = 50)
    private String generalTicketId;

    @Column(name = "classification", length = 50)
    @Builder.Default
    private String classification = "INTAKE";

    @Column(name = "complaint_classification", length = 50)
    @Builder.Default
    private String priority = "General"; // Highly Sensitive, Sensitive, General

    @Column(name = "requires_investigation")
    @Builder.Default
    private Boolean requiresInvestigation = false;

    @Column(name = "investigation_type", length = 100)
    private String investigationType; // CUSTOMER_ACCOUNT, LOAN, IBD, DIGITAL_BANKING, GENERAL

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "fcr_status")
    private Boolean fcrStatus;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = ComplaintStatus.RECORDED.name(); // RECORDED, ON_TRACK, ESCALATED, RESOLVED, CLOSED, DECLINED

    @Transient
    private String overallStatus;

    // ─── Stage SLA Lifecycle Tracking ───
    @Column(name = "current_stage", length = 100)
    private String currentStage; // CMD_SCREENING, FORWARDING, SERVICE_QUALITY, CXO_REVIEW, CEO_DIRECTION,
                                 // INVESTIGATION, COMMITTEE_REVIEW, RESOLUTION, NOTIFICATION

    @Column(name = "current_stage_started_at")
    private LocalDateTime currentStageStartedAt;

    @Column(name = "current_stage_due_time")
    private LocalDateTime currentStageDueTime;

    @Column(name = "current_stage_allowed_minutes")
    private Integer currentStageAllowedMinutes;

    @Column(name = "current_stage_elapsed_minutes")
    @Builder.Default
    private Integer currentStageElapsedMinutes = 0;

    @Column(name = "current_stage_status", length = 30)
    @Builder.Default
    private String currentStageStatus = "ON_TRACK"; // ON_TRACK, APPROACHING, BREACHED

    // ─── Overall Case SLA Lifecycle Tracking ───
    @Column(name = "overall_sla_start_time")
    private LocalDateTime overallSlaStartTime;

    @Column(name = "overall_sla_due_time")
    private LocalDateTime overallSlaDueTime;

    @Column(name = "total_allowed_minutes")
    private Integer totalAllowedMinutes; // Business minutes allowed overall

    @Column(name = "total_elapsed_minutes")
    @Builder.Default
    private Integer totalElapsedMinutes = 0; // Business minutes elapsed overall

    @Column(name = "remaining_minutes")
    private Integer remainingMinutes;

    @Column(name = "sla_status", length = 30)
    private String slaStatus; // ON_TRACK, APPROACHING, BREACHED, ESCALATED, RESOLVED_WITHIN_SLA,
                              // RESOLVED_AFTER_SLA

    @Column(name = "breached")
    @Builder.Default
    private Boolean breached = false;

    @Column(name = "deadline")
    private LocalDateTime deadline; // Backward compatibility alias for overallSlaDueTime

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "breached_at")
    private LocalDateTime breachedAt;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0; // 0=Work Unit, 1=Dept Manager/CMD Manager, 2=Director, 3=Chief Officer,
                                         // 4=Executive

    @Column(name = "breach_reason", length = 255)
    private String breachReason;

    // Automated Reminder Flags
    @Column(name = "reminder_1_sent")
    @Builder.Default
    private Boolean reminder1Sent = false; // 80% SLA

    @Column(name = "reminder_2_sent")
    @Builder.Default
    private Boolean reminder2Sent = false; // 100% SLA

    @Column(name = "reminder_3_sent")
    @Builder.Default
    private Boolean reminder3Sent = false; // Breach & Escalation

    // Duration in business minutes spent in each lane/department
    @Column(name = "branch_staff_duration")
    @Builder.Default
    private Integer branchStaffDuration = 0;

    @Column(name = "cmd_duration")
    @Builder.Default
    private Integer cmdDuration = 0;

    @Column(name = "audit_duration")
    @Builder.Default
    private Integer auditDuration = 0;

    @Column(name = "department_duration")
    @Builder.Default
    private Integer departmentDuration = 0;

    @Column(name = "service_quality_duration")
    @Builder.Default
    private Integer serviceQualityDuration = 0;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "manager", length = 100)
    private String manager;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "dbc_ticket_id", length = 100)
    private String dbcTicketId;

    public String getDbcTicketId() {
        if (this.dbcTicketId != null && this.dbcTicketId.startsWith("DBC-")) {
            return this.dbcTicketId;
        }
        if (this.complaintId != null && this.complaintId.startsWith("DBC-")) {
            return this.complaintId;
        }
        if (this.generalTicketId != null && this.generalTicketId.startsWith("DBC-")) {
            return this.generalTicketId;
        }
        return this.dbcTicketId != null ? this.dbcTicketId : this.complaintId;
    }

    public String getStaffHandling() {
        return this.manager != null ? this.manager : "Unassigned";
    }

    public LocalDateTime getExpectedResolutionDate() {
        return this.overallSlaDueTime != null ? this.overallSlaDueTime : this.deadline;
    }
}
