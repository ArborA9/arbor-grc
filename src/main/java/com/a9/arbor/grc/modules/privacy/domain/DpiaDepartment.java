package com.a9.arbor.grc.modules.privacy.domain;

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
@Table(name = "dpia_department")
@Getter
@Setter
public class DpiaDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dpia_project_id", nullable = false)
    private Long dpiaProjectId;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "reason_description", length = 4000)
    private String reasonDescription;

    @Column(name = "questionnaire_id")
    private Long questionnaireId;

    @Column(name = "champion_user_id")
    private Long championUserId;

    @Column(name = "department_head_user_id")
    private Long departmentHeadUserId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DpiaDepartmentStatus status = DpiaDepartmentStatus.NOT_STARTED;

    @Column(name = "return_from", length = 16)
    private String returnFrom;

    @Column(name = "return_reason", length = 4000)
    private String returnReason;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Column(name = "head_work_task_id")
    private Long headWorkTaskId;

    @Column(name = "head_notes", length = 4000)
    private String headNotes;

    @Column(name = "head_decided_at")
    private Instant headDecidedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "head_outcome", length = 32)
    private DpiaHeadOutcome headOutcome = DpiaHeadOutcome.PENDING;

    @Column(name = "dpo_accepted_at")
    private Instant dpoAcceptedAt;

    @Column(name = "dpo_accepted_by_user_id")
    private Long dpoAcceptedByUserId;

    @Column(name = "assessment_complete", nullable = false)
    private boolean assessmentComplete;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

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
