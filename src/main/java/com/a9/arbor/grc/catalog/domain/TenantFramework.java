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

@Entity
@Table(name = "tenant_framework", uniqueConstraints = @UniqueConstraint(
        name = "uq_tenant_framework", columnNames = {"tenant_id", "framework_id"}))
@Getter
@Setter
public class TenantFramework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "activated_by")
    private String activatedBy;

    /** When false, CLOUD sheet nodes are excluded from SoA/Gap seed (V2 Phase 4). */
    @Column(name = "include_cloud", nullable = false)
    private boolean includeCloud;
}
