package com.a9.arbor.grc.modules.evidence.domain;

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
import java.time.LocalDate;

@Entity
@Table(name = "evidence")
@Getter
@Setter
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceStatus status = EvidenceStatus.ACTIVE;

    private LocalDate expiresOn;

    @Column(nullable = false)
    private int currentVersion = 1;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;

    /** When EVIDENCE_EXPIRING was last sent (once per expiry window). */
    @Column(name = "expiry_notified_at")
    private Instant expiryNotifiedAt;

    @Column(name = "expired_notified_at")
    private Instant expiredNotifiedAt;

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

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
