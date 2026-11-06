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

@Entity
@Table(name = "grc_audit_checklist_item")
@Getter
@Setter
public class GrcAuditChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "checklist_id", nullable = false)
    private Long checklistId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "framework_node_id")
    private Long frameworkNodeId;

    @Column(name = "node_code", nullable = false, length = 80)
    private String nodeCode;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(length = 120)
    private String section;

    @Lob
    @Column(name = "test_focus")
    private String testFocus;

    @Column(name = "evidence_module", length = 300)
    private String evidenceModule;

    @Column(name = "soa_applicability", length = 120)
    private String soaApplicability;

    @Column(name = "library_locked", nullable = false)
    private boolean libraryLocked = true;

    @Column(name = "in_scope", nullable = false)
    private boolean inScope = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ChecklistAssessment assessment;

    @Lob
    private String notes;

    @Lob
    @Column(name = "evidence_examined")
    private String evidenceExamined;

    @Column(name = "persons_interviewed", length = 300)
    private String personsInterviewed;

    @Column(name = "reviewed_date")
    private java.time.LocalDate reviewedDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ChecklistConclusion conclusion;

    @Lob
    @Column(name = "finding_description")
    private String findingDescription;

    @Column(name = "finding_id")
    private Long findingId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
