package com.a9.arbor.grc.modules.soa.domain;

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

@Entity
@Table(name = "soa")
@Getter
@Setter
public class Soa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "framework_code", nullable = false, length = 64)
    private String frameworkCode;

    @Column(name = "framework_name", nullable = false)
    private String frameworkName;

    @Column(name = "framework_version", nullable = false, length = 64)
    private String frameworkVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SoaStatus status = SoaStatus.DRAFT;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by_user_id")
    private Long completedByUserId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
