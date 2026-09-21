package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", length = 100, unique = true, nullable = false)
    private String configKey;

    @Column(name = "config_group", length = 50, nullable = false)
    private String configGroup; // INTAKE_SLA, STAGE_SLA, OVERALL_SLA, CUSTOMER_NOTIFICATION

    @Column(name = "display_name", length = 150, nullable = false)
    private String displayName;

    @Column(name = "allowed_minutes", nullable = false)
    private Integer allowedMinutes;

    @Column(name = "priority", length = 30)
    private String priority; // HIGHLY_SENSITIVE, SENSITIVE, GENERAL, ALL

    @Column(name = "department", length = 100)
    private String department; // Optional department override

    @Column(name = "description", length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
