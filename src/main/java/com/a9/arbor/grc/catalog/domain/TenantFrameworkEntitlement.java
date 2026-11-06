package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Super Admin entitlement: tenant may activate this published framework (Doc 02 T-02). */
@Entity
@Table(name = "tenant_framework_entitlement", uniqueConstraints = @UniqueConstraint(
        name = "uq_tenant_framework_entitlement", columnNames = {"tenant_id", "framework_id"}))
@Getter
@Setter
public class TenantFrameworkEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "entitled_at", nullable = false)
    private Instant entitledAt;

    @Column(name = "entitled_by")
    private String entitledBy;
}
