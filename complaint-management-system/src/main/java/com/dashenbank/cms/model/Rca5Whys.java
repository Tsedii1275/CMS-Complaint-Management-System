package com.dashenbank.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rca_5whys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rca5Whys {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rca_case_id", nullable = false)
    @JsonIgnore
    private RcaCase rcaCase;

    @Column(name = "why_1", length = 255)
    private String why1;

    @Column(name = "why_2", length = 255)
    private String why2;

    @Column(name = "why_3", length = 255)
    private String why3;

    @Column(name = "why_4", length = 255)
    private String why4;

    @Column(name = "why_5", length = 255)
    private String why5;

    @Column(name = "root_cause_statement", columnDefinition = "TEXT")
    private String rootCauseStatement;
}
