package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One import attempt per framework found in an uploaded workbook, with the raw rows in
 * {@link FrameworkImportRow}. Written on every Master Import so F4's dry-run and validation-report
 * UI is a screen on top of existing data rather than a re-plumb.
 */
@Entity
@Table(name = "framework_import_batch")
@Getter
@Setter
public class FrameworkImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_filename", nullable = false, length = 512)
    private String sourceFilename;

    @Column(name = "source_format", nullable = false, length = 32)
    private String sourceFormat;

    @Column(nullable = false, length = 32)
    private String status;

    /** Always false in F2: the dry-run branch is F4. */
    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    /** The sheet's own {@code framework_code} alias, e.g. {@code ISO27001_2022}. */
    @Column(name = "framework_code", length = 64)
    private String frameworkCode;

    @Column(name = "framework_version", length = 64)
    private String frameworkVersion;

    @Column(name = "framework_id")
    private Long frameworkId;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Lob
    @Column(name = "report")
    private String report;

    @Column(name = "started_by", length = 255)
    private String startedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
