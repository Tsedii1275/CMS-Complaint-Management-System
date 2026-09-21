package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_breach_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaBreachRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", length = 100, nullable = false)
    private String complaintId;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "stage_name", length = 100)
    private String stageName;

    @Column(name = "breach_reason", length = 255)
    private String breachReason;

    @Column(name = "breach_duration_minutes")
    private Long breachDurationMinutes;

    @Column(name = "responsible_work_unit", length = 100)
    private String responsibleWorkUnit;

    @Column(name = "escalation_actions_taken", length = 500)
    private String escalationActionsTaken;

    @CreationTimestamp
    @Column(name = "breach_timestamp", updatable = false)
    private LocalDateTime breachTimestamp;
}
