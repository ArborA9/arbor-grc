package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** One sent reminder for a tenant module licence expiry date + milestone. */
@Entity
@Table(name = "tenant_module_expiry_notice", uniqueConstraints = @UniqueConstraint(
        name = "uq_tenant_module_expiry_notice",
        columnNames = {"tenant_id", "module_code", "milestone", "expires_on"}))
@Getter
@Setter
public class TenantModuleExpiryNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 32)
    private ModuleCode moduleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "milestone", nullable = false, length = 16)
    private LicenseExpiryMilestone milestone;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
