package com.a9.arbor.grc.modules.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Singleton tenant risk matrix config (inherent + residual JSON + grid size). */
@Entity
@Table(name = "risk_matrix_config")
@Getter
@Setter
public class RiskMatrixConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "inherent_json", nullable = false)
    private String inherentJson;

    @Lob
    @Column(name = "residual_json", nullable = false)
    private String residualJson;

    @Column(name = "grid_size", nullable = false)
    private int gridSize = 5;

    @Lob
    @Column(name = "row_axis_json")
    private String rowAxisJson;

    @Lob
    @Column(name = "col_axis_json")
    private String colAxisJson;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "change_reason", length = 1000)
    private String changeReason;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
