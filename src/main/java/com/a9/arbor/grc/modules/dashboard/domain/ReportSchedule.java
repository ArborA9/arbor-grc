package com.a9.arbor.grc.modules.dashboard.domain;

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
@Table(name = "report_schedule")
@Getter
@Setter
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false, length = 64)
    private String reportId;

    @Column(nullable = false, length = 16)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportScheduleFrequency frequency;

    @Column(name = "recipient_user_ids", nullable = false, length = 255)
    private String recipientUserIds;

    @Column(name = "soa_id")
    private Long soaId;

    @Column(name = "gap_id")
    private Long gapId;

    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(length = 16)
    private String variant;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
