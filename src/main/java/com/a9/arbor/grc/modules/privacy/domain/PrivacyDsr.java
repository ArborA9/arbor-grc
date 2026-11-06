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
import java.time.LocalDate;

@Entity
@Table(name = "privacy_dsr")
@Getter
@Setter
public class PrivacyDsr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32)
    private String displayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 32)
    private DsrRequestType requestType;

    @Column(name = "subject_name", nullable = false, length = 255)
    private String subjectName;

    @Column(name = "subject_email", length = 255)
    private String subjectEmail;

    @Column(name = "subject_reference", length = 255)
    private String subjectReference;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DsrStatus status = DsrStatus.RECEIVED;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "due_at", nullable = false)
    private LocalDate dueAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_outcome", length = 32)
    private DsrCloseOutcome closeOutcome;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "day20_notified_at")
    private Instant day20NotifiedAt;

    @Column(name = "day28_notified_at")
    private Instant day28NotifiedAt;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

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
