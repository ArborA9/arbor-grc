package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** An emergency Super Admin reset code that has already been applied (stored as SHA-256). */
@Entity
@Table(name = "super_admin_reset")
@Getter
@Setter
public class SuperAdminReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, length = 64, unique = true)
    private String codeHash;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;
}
