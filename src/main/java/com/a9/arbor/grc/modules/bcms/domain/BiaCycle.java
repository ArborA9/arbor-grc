package com.a9.arbor.grc.modules.bcms.domain;

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
@Table(name = "bia_cycle")
@Getter
@Setter
public class BiaCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bia_project_id", nullable = false)
    private Long biaProjectId;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "display_id", nullable = false, length = 64)
    private String displayId;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "version_label", nullable = false, length = 32)
    private String versionLabel = "1.0";

    @Column(length = 4000)
    private String objective;

    @Column(name = "scope_text", length = 4000)
    private String scopeText;

    @Column(length = 4000)
    private String mbco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BiaStatus status = BiaStatus.DRAFT;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Column(name = "return_reason", length = 4000)
    private String returnReason;

    @Column(name = "review_due_notified_at")
    private Instant reviewDueNotifiedAt;

    @Column(name = "not_submitted_notified_at")
    private Instant notSubmittedNotifiedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "bc_head_user_id")
    private Long bcHeadUserId;

    @Column(name = "bc_head_notes", length = 4000)
    private String bcHeadNotes;

    @Column(name = "bc_head_decided_at")
    private Instant bcHeadDecidedAt;

    @Column(name = "admin_forward_notes", length = 4000)
    private String adminForwardNotes;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "bc_head_work_task_id")
    private Long bcHeadWorkTaskId;

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
