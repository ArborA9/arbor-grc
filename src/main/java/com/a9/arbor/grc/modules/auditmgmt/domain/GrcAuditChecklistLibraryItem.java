package com.a9.arbor.grc.modules.auditmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "grc_audit_checklist_library_item")
@Getter
@Setter
public class GrcAuditChecklistLibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "library_id", nullable = false)
    private Long libraryId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "clause_ref", nullable = false, length = 80)
    private String clauseRef;

    @Column(length = 120)
    private String section;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    @Column(name = "test_focus")
    private String testFocus;

    @Column(name = "soa_applicability", length = 120)
    private String soaApplicability;

    @Column(name = "evidence_lives", length = 300)
    private String evidenceLives;

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
