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
@Table(name = "grc_audit_finding")
@Getter
@Setter
public class GrcAuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "checklist_item_id")
    private Long checklistItemId;

    @Column(name = "display_code", nullable = false, length = 64)
    private String displayCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FindingSeverity severity;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Lob
    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "control_ref", length = 100)
    private String controlRef;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "capa_id")
    private Long capaId;

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
