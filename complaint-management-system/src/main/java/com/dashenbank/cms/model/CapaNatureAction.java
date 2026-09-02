package com.dashenbank.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "capa_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CapaNatureAction {

    public static final String TYPE_CORRECTIVE = "CORRECTIVE";
    public static final String TYPE_PREVENTIVE = "PREVENTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capa_analysis_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CapaAnalysis capaAnalysis;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "action_text", columnDefinition = "TEXT")
    private String actionText;

    @Column(name = "responsible_department", length = 255)
    private String responsibleDepartment;

    @Column(name = "responsible_officer", length = 255)
    private String responsibleOfficer;
}
