package com.a9.arbor.grc.modules.capa.domain;

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
@Table(name = "capa")
@Getter
@Setter
public class Capa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "register_code", nullable = false, length = 32)
    private CapaRegister registerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private CapaSourceType sourceType;

    @Column(name = "source_record_id")
    private Long sourceRecordId;

    /** Finding ID typed for a CAPA whose audit was not done in Arbor. */
    @Column(name = "external_finding_code", length = 80)
    private String externalFindingCode;

    /** Typed source when registerCode is OTHER. */
    @Column(name = "source_other", length = 120)
    private String sourceOther;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    private String description;

    @Lob
    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "control_ref", length = 100)
    private String controlRef;

    @Column(length = 32)
    private String severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CapaPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CapaStatus status = CapaStatus.OPEN;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    /** Excel col J: Date Assigned - when the CAPA was assigned to a contributor */
    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Lob
    @Column(name = "root_cause")
    private String rootCause;

    @Lob
    @Column(name = "corrective_action")
    private String correctiveAction;

    @Lob
    private String correction;

    @Lob
    @Column(name = "evidence_required")
    private String evidenceRequired;

    @Column(name = "contributor_department", length = 120)
    private String contributorDepartment;

    /** Excel journey STEP 4: Audit Title / project this CAPA belongs to. */
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "capa_register_id")
    private Long capaRegisterId;

    /** Excel journey STEP 4: Conducted by. */
    @Column(name = "conducted_by", length = 200)
    private String conductedBy;

    @Lob
    @Column(name = "preventive_action")
    private String preventiveAction;

    @Lob
    @Column(name = "progress_notes")
    private String progressNotes;

    /** Excel col O: Response Description - filled by Contributor */
    @Lob
    @Column(name = "response_description")
    private String responseDescription;

    /** Excel col P: Upload Evidence - reference/note (actual files via evidence library) */
    @Lob
    @Column(name = "contributor_evidence_note")
    private String contributorEvidenceNote;

    /** Excel col Q: Corrective Action plan review remarks - Admin fills */
    @Lob
    @Column(name = "review_remarks")
    private String reviewRemarks;

    /** Excel col R: Evidence of Completion - Admin fills */
    @Lob
    @Column(name = "evidence_of_completion")
    private String evidenceOfCompletion;

    /** Excel col S: Verification Date - Admin fills */
    @Column(name = "verification_date")
    private LocalDate verificationDate;

    /** Excel col T: Verification Result - Admin fills (PASS, FAIL, PARTIAL, etc.) */
    @Column(name = "verification_result", length = 32)
    private String verificationResult;

    @Lob
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "due_soon_notified_at")
    private Instant dueSoonNotifiedAt;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Column(name = "escalation_notified_at")
    private Instant escalationNotifiedAt;

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
