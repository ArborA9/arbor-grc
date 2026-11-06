package com.a9.arbor.grc.modules.iam.domain;

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
 * Local TOTP MFA enrollment (Path C B4). One row per user; {@code enabled} flips to {@code true}
 * only after the user confirms a code, so a half-finished enrollment never blocks login.
 */
@Entity
@Table(name = "user_mfa_totp")
@Getter
@Setter
public class UserMfaTotp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

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
