package com.a9.arbor.grc.modules.bcms.domain;

import com.a9.arbor.grc.modules.bcms.pack.BiaImpactEngine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bia_impact_cell")
@Getter
@Setter
public class BiaImpactCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BiaImpactEngine.Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BiaImpactEngine.Band band;

    private Integer rating;

    @Column(length = 2000)
    private String remarks;
}
