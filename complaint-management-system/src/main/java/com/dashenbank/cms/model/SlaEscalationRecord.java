package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_escalation_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaEscalationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", length = 100, nullable = false)
    private String complaintId;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "from_level")
    private Integer fromLevel;

    @Column(name = "to_level")
    private Integer toLevel;

    @Column(name = "escalated_from_unit", length = 100)
    private String escalatedFromUnit;

    @Column(name = "escalated_to_role", length = 100)
    private String escalatedToRole;

    @Column(name = "escalated_to_user", length = 100)
    private String escalatedToUser;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "escalation_timestamp", updatable = false)
    private LocalDateTime escalationTimestamp;
}
