package com.a9.arbor.grc.catalog.excel;

/**
 * One raw source row bound for {@code framework_import_row}, shared by every framework parser.
 *
 * <p>Kept verbatim (as {@code rawJson}) whatever the outcome, so a row that was rejected or attached
 * somewhere lossy can still be handed back to the framework author without re-reading the workbook.
 */
public record ImportStagingRow(
        String sheetName,
        int rowIndex,
        Kind kind,
        String rawJson,
        Status status,
        String message
) {

    /** {@code REFERENCE} is a reference-pack row: attributes and a question block on one line. */
    public enum Kind { NODE, QUESTION, REFERENCE, BANNER, PLACEHOLDER }

    public enum Status { OK, ERROR, SKIPPED }
}
