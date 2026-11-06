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
@Table(name = "audit_access_grant")
@Getter
@Setter
public class AuditAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32, unique = true)
    private String displayId;

    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grantee_type", nullable = false, length = 32)
    private AuditGranteeType granteeType;

    @Column(length = 120)
    private String organisation;

    @Column(name = "nda_reference", length = 60)
    private String ndaReference;

    @Lob
    @Column(name = "modules_json", nullable = false)
    private String modulesJson;

    @Column(name = "scope_boundary", length = 500)
    private String scopeBoundary;

    @Column(name = "can_preview_evidence", nullable = false)
    private boolean canPreviewEvidence = true;

    @Column(name = "can_export_notes", nullable = false)
    private boolean canExportNotes = true;

    @Column(name = "can_download_evidence", nullable = false)
    private boolean canDownloadEvidence;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "grace_days", nullable = false)
    private int graceDays = 14;

    @Column(nullable = false)
    private boolean revoked;

    @Lob
    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

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

    public LocalDate autoExpiry() {
        return validTo.plusDays(graceDays);
    }

    public boolean activeOn(LocalDate day) {
        if (revoked) {
            return false;
        }
        if (day.isBefore(validFrom)) {
            return false;
        }
        return !day.isAfter(autoExpiry());
    }
}
