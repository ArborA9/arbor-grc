package com.a9.arbor.grc.modules.privacy.domain;

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

import java.time.Instant;

@Entity
@Table(name = "privacy_breach")
@Getter
@Setter
public class PrivacyBreach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32)
    private String displayId;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BreachSeverity severity = BreachSeverity.MEDIUM;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    @Column(name = "report_deadline_at", nullable = false)
    private Instant reportDeadlineAt;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "authority_reference", length = 255)
    private String authorityReference;

    @Column(name = "affected_subjects_estimate", length = 255)
    private String affectedSubjectsEstimate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BreachStatus status = BreachStatus.OPEN;

    @Column(name = "hour48_notified_at")
    private Instant hour48NotifiedAt;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

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
