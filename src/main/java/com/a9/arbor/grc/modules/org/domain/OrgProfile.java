package com.a9.arbor.grc.modules.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "org_profile")
@Getter
@Setter
public class OrgProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(length = 100)
    private String industry;

    @Column(name = "size_band", length = 64)
    private String sizeBand;

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Dubai";

    @Column(name = "date_format", nullable = false, length = 32)
    private String dateFormat = "DD/MM/YYYY";

    @Column(name = "wizard_completed_at")
    private Instant wizardCompletedAt;

    @Column(name = "risk_matrix_grid_size", nullable = false)
    private int riskMatrixGridSize = 5;

    @Column(name = "risk_matrix_locked", nullable = false)
    private boolean riskMatrixLocked;

    @Column(name = "risk_appetite_rating", nullable = false, length = 16)
    private String riskAppetiteRating = "LOW";

    /** SPA idle logout threshold, minutes (Path C B3). Default 5, capped 1-480 at the API layer. */
    @Column(name = "idle_timeout_minutes", nullable = false)
    private int idleTimeoutMinutes = 5;

    /** BCMS RTO thresholds by BIA criticality: hours above which rtoJustification is required. */
    @Column(name = "rto_max_critical_hours", nullable = false)
    private int rtoMaxCriticalHours = 4;

    @Column(name = "rto_max_high_hours", nullable = false)
    private int rtoMaxHighHours = 24;

    @Column(name = "rto_max_medium_hours", nullable = false)
    private int rtoMaxMediumHours = 72;

    @Column(name = "rto_max_low_hours", nullable = false)
    private int rtoMaxLowHours = 168;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public boolean isWizardCompleted() {
        return wizardCompletedAt != null;
    }
}
