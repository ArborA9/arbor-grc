package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Local TOTP MFA for a Super Admin (catalog DB). One row per admin; {@code enabled} flips to
 * {@code true} only after the user confirms a code, so a half-finished enrollment never blocks login.
 */
@Entity
@Table(name = "super_admin_mfa_totp")
@Getter
@Setter
public class SuperAdminMfaTotp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "super_admin_id", nullable = false, unique = true)
    private Long superAdminId;

    /** AES-GCM ciphertext (Base64) of the Base32 TOTP secret: never stored in plaintext. */
    @Column(name = "secret_encrypted", nullable = false, length = 500)
    private String secretEncrypted;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
