package com.a9.arbor.grc.modules.iam.domain;

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
 * Maps an IdP (Entra) group name/object-id to an Arbor {@link Role}. Applied on every SSO login
 * (Path C B2) as a <b>union</b> with the user's Arbor-assigned roles: never removes roles that an
 * Admin assigned directly. Tenant-scoped since group names are specific to that tenant's directory.
 */
@Entity
@Table(name = "sso_group_role_mapping")
@Getter
@Setter
public class SsoGroupRoleMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The IdP group name or object id, as it appears in the OIDC {@code groups} claim. */
    @Column(name = "idp_group_name", nullable = false, length = 255)
    private String idpGroupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
