package com.a9.arbor.grc.modules.gap.domain;

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
@Table(name = "gap_assessment")
@Getter
@Setter
public class GapAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "framework_code", nullable = false, length = 64)
    private String frameworkCode;

    @Column(name = "framework_name", nullable = false)
    private String frameworkName;

    @Column(name = "framework_version", nullable = false, length = 64)
    private String frameworkVersion;

    @Column(name = "soa_id")
    private Long soaId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 32)
    private GapAssessmentType assessmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GapAssessmentStatus status = GapAssessmentStatus.DRAFT;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Lob
    private String scope;

    @Column(name = "assessor_user_id")
    private Long assessorUserId;

    /**
     * UAE IA only: 4 = seed Gap-routed sub-controls (L4); 3 = seed L3 controls only.
     * Other frameworks always behave as 4. Immutable after create.
     */
    @Column(name = "assessment_depth", nullable = false)
    private int assessmentDepth = 4;

    @Column(name = "overall_score", precision = 6, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "rag_status", length = 32)
    private String ragStatus;

    @Column(name = "applicable_count", nullable = false)
    private int applicableCount;

    @Column(name = "scored_count", nullable = false)
    private int scoredCount;

    /** UAE IA three-layer snapshot (Management / Technical / domains): JSON. */
    @Lob
    @Column(name = "layer_score_json")
    private String layerScoreJson;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by_user_id")
    private Long completedByUserId;

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
