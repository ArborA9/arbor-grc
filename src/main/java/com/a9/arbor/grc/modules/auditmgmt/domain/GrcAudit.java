package com.a9.arbor.grc.modules.auditmgmt.domain;

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
@Table(name = "grc_audit")
@Getter
@Setter
public class GrcAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 20, unique = true)
    private String displayId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 32)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_mode", nullable = false, length = 32)
    private AuditMode auditMode = AuditMode.COMPLIANCE;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_standard", length = 32)
    private AuditStandard auditStandard = AuditStandard.ISO_27001;

    @Column(name = "framework_id")
    private Long frameworkId;

    @Column(name = "framework_code", nullable = false, length = 64)
    private String frameworkCode;

    @Column(name = "framework_name", nullable = false)
    private String frameworkName;

    @Column(name = "framework_version", nullable = false, length = 64)
    private String frameworkVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GrcAuditStatus status = GrcAuditStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "auditor_organisation", length = 120)
    private String auditorOrganisation;

    @Column(name = "auditee_org_unit_id")
    private Long auditeeOrgUnitId;

    @Column(name = "grace_days", nullable = false)
    private int graceDays = 14;

    @Lob
    @Column(name = "overall_conclusion")
    private String overallConclusion;

    @Column(name = "report_issued_date")
    private LocalDate reportIssuedDate;

    @Lob
    @Column(name = "scope_notes")
    private String scopeNotes;

    @Lob
    @Column(name = "executive_summary")
    private String executiveSummary;

    @Column(name = "lead_auditor_user_id", nullable = false)
    private Long leadAuditorUserId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "auditor_signed_at")
    private Instant auditorSignedAt;

    @Column(name = "auditor_signed_by_user_id")
    private Long auditorSignedByUserId;

    @Column(name = "admin_signed_at")
    private Instant adminSignedAt;

    @Column(name = "admin_signed_by_user_id")
    private Long adminSignedByUserId;

    @Lob
    @Column(name = "return_reason")
    private String returnReason;

    @Column(nullable = false)
    private boolean locked;

    @Column(name = "report_storage_key", length = 512)
    private String reportStorageKey;

    @Column(name = "report_generated_at")
    private Instant reportGeneratedAt;

    @Column(name = "gap_assessment_id")
    private Long gapAssessmentId;

    @Column(name = "start_notified_at")
    private Instant startNotifiedAt;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Column(name = "report_pending_notified_at")
    private Instant reportPendingNotifiedAt;

    @Lob
    @Column(name = "domain_narrative_json")
    private String domainNarrativeJson;

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
