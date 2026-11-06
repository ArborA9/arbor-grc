package com.a9.arbor.grc.modules.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Stage note on a risk: deletable; optional Internal Only (§13.7). */
@Entity
@Table(name = "risk_stage_note")
@Getter
@Setter
public class RiskStageNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false)
    private Long riskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_stage", nullable = false, length = 32)
    private RiskStatus workflowStage;

    @Lob
    @Column(nullable = false)
    private String body;

    @Column(name = "internal_only", nullable = false)
    private boolean internalOnly;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
