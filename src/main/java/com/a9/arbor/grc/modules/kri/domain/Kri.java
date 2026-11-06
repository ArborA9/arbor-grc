package com.a9.arbor.grc.modules.kri.domain;

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
@Table(name = "kri")
@Getter
@Setter
public class Kri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32, unique = true)
    private String displayId;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "linked_risk_id")
    private Long linkedRiskId;

    @Lob
    @Column(name = "metric_description")
    private String metricDescription;

    @Column(length = 64)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KriFrequency frequency;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KriDirection direction;

    @Column(name = "green_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal greenThreshold;

    @Column(name = "amber_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal amberThreshold;

    @Column(name = "red_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal redThreshold;

    @Column(name = "current_value", precision = 18, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "previous_value", precision = 18, scale = 4)
    private BigDecimal previousValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private KriRag rag;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private KriTrend trend;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "last_measured_at")
    private Instant lastMeasuredAt;

    @Column(name = "due_notified_at")
    private Instant dueNotifiedAt;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Column(name = "red_acknowledged_at")
    private Instant redAcknowledgedAt;

    @Column(name = "red_acknowledged_by")
    private Long redAcknowledgedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KriStatus status = KriStatus.ACTIVE;

    @Column(name = "work_task_id")
    private Long workTaskId;

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
