package com.a9.arbor.grc.modules.policy.domain;

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
@Table(name = "policy")
@Getter
@Setter
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyStatus status = PolicyStatus.DRAFT;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "major_version", nullable = false)
    private int majorVersion;

    @Column(name = "minor_version", nullable = false)
    private int minorVersion;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "ack_required", nullable = false)
    private boolean ackRequired = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "ack_scope_type", nullable = false, length = 32)
    private PolicyAckScopeType ackScopeType = PolicyAckScopeType.ALL_ACTIVE_USERS;

    @Column(name = "ack_deadline")
    private LocalDate ackDeadline;

    @Lob
    @Column(name = "reject_reason")
    private String rejectReason;

    @Lob
    @Column(name = "review_notes")
    private String reviewNotes;

    @Lob
    @Column(name = "body_draft")
    private String bodyDraft;

    @Column(name = "storage_key_draft", length = 512)
    private String storageKeyDraft;

    @Column(name = "original_filename_draft", length = 255)
    private String originalFilenameDraft;

    @Column(name = "content_type_draft", length = 128)
    private String contentTypeDraft;

    @Column(name = "size_bytes_draft")
    private Long sizeBytesDraft;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "review_due_notified_at")
    private Instant reviewDueNotifiedAt;

    @Column(name = "expired_notified_at")
    private Instant expiredNotifiedAt;

    @Column(name = "ack_deadline_notified_at")
    private Instant ackDeadlineNotifiedAt;

    @Column(name = "ack_overdue_notified_at")
    private Instant ackOverdueNotifiedAt;

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
