package com.a9.arbor.grc.modules.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "risk_project")
@Getter
@Setter
public class RiskProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 16)
    private RiskType projectType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "assessment_year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskProjectStatus status = RiskProjectStatus.ACTIVE;

    /** IS projects only: used to seed one stub risk per control. */
    @Column(name = "framework_id")
    private Long frameworkId;

    @Column(name = "seed_total", nullable = false)
    private int seedTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_status", nullable = false, length = 16)
    private RiskProjectSeedStatus seedStatus = RiskProjectSeedStatus.DONE;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

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
