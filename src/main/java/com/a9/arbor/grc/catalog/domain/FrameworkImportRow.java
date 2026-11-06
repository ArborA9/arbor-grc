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

/** One source spreadsheet row, kept verbatim so an import can be diffed and re-reported. */
@Entity
@Table(name = "framework_import_row")
@Getter
@Setter
public class FrameworkImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "sheet_name", length = 255)
    private String sheetName;

    /** 1-based Excel row number, so report lines match what the client sees in the file. */
    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "row_kind", length = 32)
    private String rowKind;

    @Lob
    @Column(name = "raw_json")
    private String rawJson;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 1024)
    private String message;
}
