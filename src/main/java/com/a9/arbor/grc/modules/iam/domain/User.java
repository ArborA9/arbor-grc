package com.a9.arbor.grc.modules.iam.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A tenant-scoped user. Lives in the tenant's own database, so email uniqueness is per-tenant.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * BCrypt hash for LOCAL accounts. {@code null} for SSO accounts: their credentials live in the
     * tenant's AD / Entra ID and are never stored here.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** How this user authenticates. Identity from AD for SSO; roles are always managed in Arbor. */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 16)
    private AuthType authType = AuthType.LOCAL;

    /**
     * Password sign-in is allowed only when this is true and {@link #passwordHash} is set.
     * Independent of {@link #authType} so one email can use both SSO and a local password.
     */
    @Column(name = "local_login_enabled", nullable = false)
    private boolean localLoginEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status = UserStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Incremented to invalidate outstanding JWTs (disable, role change, password reset). */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    /**
     * When true, {@code user_admin_module} is the Admin's module list (it may be empty).
     * When false, the first request copies the modules licensed for the tenant so existing
     * Admins keep the access they already had.
     */
    @Column(name = "admin_modules_set", nullable = false)
    private boolean adminModulesSet = false;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
