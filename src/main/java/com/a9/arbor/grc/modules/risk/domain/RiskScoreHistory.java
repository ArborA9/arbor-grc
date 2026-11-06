package com.a9.arbor.grc.modules.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Immutable scoring snapshot whenever P/I/CE/residual change (§13.7). */
@Entity
@Table(name = "risk_score_history")
@Getter
@Setter
public class RiskScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false)
    private Long riskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskLevel probability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskLevel impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_effectiveness", nullable = false, length = 16)
    private RiskLevel controlEffectiveness;

    @Enumerated(EnumType.STRING)
    @Column(name = "inherent_risk", nullable = false, length = 16)
    private RiskLevel inherentRisk;

    @Enumerated(EnumType.STRING)
    @Column(name = "residual_risk", nullable = false, length = 16)
    private RiskLevel residualRisk;

    @Column(length = 1000)
    private String reason;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void onCreate() {
        recordedAt = Instant.now();
    }
}
