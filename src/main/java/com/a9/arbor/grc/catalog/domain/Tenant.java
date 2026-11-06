package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Registry entry for a tenant. Lives in the shared <b>catalog</b> database and records how to reach
 * the tenant's own (physically separate) database.
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 64)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status;

    @Column(name = "jdbc_url", nullable = false, length = 1000)
    private String jdbcUrl;

    @Column(name = "db_username", nullable = false)
    private String dbUsername;

    @Column(name = "db_password", nullable = false, length = 500)
    private String dbPassword;

    // --- SSO / AD (OIDC) configuration. Set at onboarding by the Arbor (Super Admin) team. ---
    // When enabled, tenant users authenticate against their own Entra ID / AD; Arbor manages roles.

    @Column(name = "sso_enabled", nullable = false)
    private boolean ssoEnabled = false;

    /** OIDC issuer URL (e.g. https://login.microsoftonline.com/<tenant-guid>/v2.0). */
    @Column(name = "oidc_issuer", length = 1000)
    private String oidcIssuer;

    @Column(name = "oidc_client_id", length = 255)
    private String oidcClientId;

    /** OIDC client secret. TODO: move to Azure Key Vault; stored here only for local dev. */
    @Column(name = "oidc_client_secret", length = 500)
    private String oidcClientSecret;

    // --- JIT provisioning (Path C B1). Only meaningful when sso_enabled is true. ---

    /** When true, an SSO login with no pre-invited Arbor account is auto-provisioned instead of denied. */
    @Column(name = "jit_enabled", nullable = false)
    private boolean jitEnabled = false;

    /** What role a JIT-created user gets ({@code BLOCK} = keep denying even if jitEnabled). */
    @Enumerated(EnumType.STRING)
    @Column(name = "jit_default_role", nullable = false, length = 32)
    private JitDefaultRole jitDefaultRole = JitDefaultRole.BLOCK;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
