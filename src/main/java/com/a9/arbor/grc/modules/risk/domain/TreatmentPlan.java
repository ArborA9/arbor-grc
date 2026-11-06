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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "treatment_plan")
@Getter
@Setter
public class TreatmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false, unique = true)
    private Long riskId;

    @Lob
    @Column(name = "action_description")
    private String actionDescription;

    @Column(name = "action_owner_id")
    private Long actionOwnerId;

    /** Doc 02 R-10: control / evidence custodian (alongside action owner). */
    @Column(name = "custodian_user_id")
    private Long custodianUserId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Lob
    @Column(name = "resources_required")
    private String resourcesRequired;

    @Column(name = "cost_estimate_aed", precision = 14, scale = 2)
    private BigDecimal costEstimateAed;

    @Lob
    @Column(name = "evidence_required")
    private String evidenceRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TreatmentPlanStatus status = TreatmentPlanStatus.NOT_STARTED;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
