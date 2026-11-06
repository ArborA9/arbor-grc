package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "risk_template")
@Getter
@Setter
public class RiskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "framework_node_id")
    private Long frameworkNodeId;

    @Column(name = "source_risk_id", nullable = false, length = 64)
    private String sourceRiskId;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    @Column
    private String description;

    @Column(nullable = false, length = 32)
    private String category;

    @Lob
    @Column(name = "typical_threats")
    private String typicalThreats;

    @Column(name = "suggested_probability", nullable = false, length = 16)
    private String suggestedProbability;

    @Column(name = "suggested_impact", nullable = false, length = 16)
    private String suggestedImpact;

    @Column(name = "suggested_control_effectiveness", nullable = false, length = 16)
    private String suggestedControlEffectiveness;

    @Column(name = "treatment_option", length = 32)
    private String treatmentOption;

    @Lob
    @Column(name = "treatment_recommendation")
    private String treatmentRecommendation;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
