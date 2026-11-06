package com.a9.arbor.grc.modules.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Insert-only audit row (req §8). Never update or delete via the application.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_role", length = 255)
    private String userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private AuditActionType actionType;

    @Column(name = "module", nullable = false, length = 64)
    private String module;

    @Column(name = "record_type", length = 64)
    private String recordType;

    @Column(name = "record_id", length = 64)
    private String recordId;

    @Column(name = "record_name", length = 500)
    private String recordName;

    @Column(name = "field_changed", length = 255)
    private String fieldChanged;

    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Column(name = "new_value", length = 2000)
    private String newValue;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", length = 16)
    private AuditAuthMethod authMethod;

    /**
     * Whether MFA was verified for this event (Path C B4). {@code null} when not applicable
     * (non-login events); {@code false} means a login happened without MFA.
     */
    @Column(name = "mfa_verified")
    private Boolean mfaVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
