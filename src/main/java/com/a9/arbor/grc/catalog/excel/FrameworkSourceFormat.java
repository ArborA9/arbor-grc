package com.a9.arbor.grc.catalog.excel;

/**
 * Workbook layouts the Super Admin framework importer accepts. Detected from sheet shape, never
 * from the filename, because clients rename the files.
 */
public enum FrameworkSourceFormat {

    /**
     * S1 — {@code Arbor_GRC_Framework_Master_Import.xlsx}: paired {@code *_Framework} /
     * {@code *_Questions} sheets, headers on Excel row 3, wide level1–level4 hierarchy columns and
     * per-node module routing flags.
     */
    MASTER_IMPORT,

    /**
     * S4 — the client reference packs ({@code Arbor_GRC_ISO27001_Reference.xlsx} and friends): a
     * clause/control ref, "What It Means", "Evidence" and a {@code Q1..Qn} question block per row.
     * Enriches a framework the Master import already created; it never creates one.
     */
    REFERENCE_PACK,

    /**
     * S2 — {@code Arbor_GRC_Framework_PCIDSS-4.0.1.xlsx}: consolidated master sheet with
     * {@code Req ID | Requirement Text | Testing Procedure | Customized Approach Objective |
     * Gap Assessment Question | Applicability | Evidence Examples}. One question per row; hierarchy
     * is derived from requirement-family and dotted req IDs.
     */
    PCI_DSS_PACK,

    /**
     * S3 — {@code Arbor_GRC_Framework_NIST-SP-800-53-Rev5.xlsx}: 20 family tabs with
     * {@code Control ID | Title | Control Statement | Discussion | Gap Assessment Question |
     * Related Controls | Baseline (L/M/H)}. Withdrawn-controls tab is skipped.
     */
    NIST_800_53_PACK,

    /** Legacy shared GRC Checklist workbook — one sheet per framework, flat leaf rows. */
    GRC_CHECKLIST,

    /** Legacy Arbor canonical {@code Controls} template. */
    ARBOR_CONTROLS
}
