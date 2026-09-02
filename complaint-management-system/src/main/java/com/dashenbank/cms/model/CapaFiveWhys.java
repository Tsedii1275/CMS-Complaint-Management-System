package com.dashenbank.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "capa_five_whys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CapaFiveWhys {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capa_analysis_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CapaAnalysis capaAnalysis;

    @Column(name = "why_1", columnDefinition = "TEXT")
    private String why1;

    @Column(name = "why_2", columnDefinition = "TEXT")
    private String why2;

    @Column(name = "why_3", columnDefinition = "TEXT")
    private String why3;

    @Column(name = "why_4", columnDefinition = "TEXT")
    private String why4;

    @Column(name = "why_5", columnDefinition = "TEXT")
    private String why5;
}
