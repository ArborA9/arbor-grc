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

@Entity
@Table(name = "framework", uniqueConstraints = @UniqueConstraint(
        name = "uq_framework_code_version", columnNames = {"code", "version"}))
@Getter
@Setter
public class Framework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 64)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 32)
    private ModuleCode moduleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FrameworkStatus status;

    @Column(name = "control_count", nullable = false)
    private int controlCount;

    @Column(name = "imported_by")
    private String importedBy;

    @Column(name = "imported_at")
    private Instant importedAt;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "published_by")
    private String publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "source_filename")
    private String sourceFilename;
}
