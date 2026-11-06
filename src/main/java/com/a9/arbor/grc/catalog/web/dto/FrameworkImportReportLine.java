package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.excel.ImportRowError;

import java.util.List;

/**
 * Per-framework outcome of one upload. A Master Import workbook carries seven frameworks plus a
 * placeholder sheet, so a single HTTP 200 can mean "six imported, one skipped, three rows rejected" -
 * the response has to say which. F4 renders this in the Super Admin console.
 *
 * @param status       {@code IMPORTED}, {@code IMPORTED_WITH_ERRORS}, {@code ENRICHED} or
 *                     {@code SKIPPED}
 * @param frameworkCode the catalog code ({@code FrameworkTypeCode} name), null when skipped
 * @param sourceFrameworkCode the sheet's own alias, e.g. {@code ISO27001_2022}
 * @param contentRows  source data rows, excluding header and banner rows
 * @param nodesCreated total {@code framework_node} rows, ancestors included; zero for a reference
 *                     pack, which never creates nodes
 * @param leafNodes    nodes routed to at least one module: one per accepted content row
 * @param routing      per-flag leaf counts, so an import can be reconciled against the client's
 *                     own per-sheet totals without reading the catalog back
 * @param merge        reference-pack join outcome, null for the other formats
 * @param info         transformations applied, e.g. node codes taken from {@code official_ref}, or
 *                     each ancestor attachment
 */
public record FrameworkImportReportLine(
        String sheetName,
        String sourceFrameworkCode,
        String frameworkCode,
        String version,
        String status,
        Long batchId,
        Long frameworkId,
        int contentRows,
        int nodesCreated,
        int leafNodes,
        int questionsImported,
        int errorCount,
        RoutingCounts routing,
        MergeCounts merge,
        List<ImportRowError> errors,
        List<String> info,
        String message
) {

    public record RoutingCounts(int inSoa, int inGapAssessment, int inAuditChecklist, int inRiskRegister) {
    }

    /**
     * @param refRows             pack rows carrying a ref
     * @param exactMatches        refs that are a node code: the pack replaces those questions
     * @param ancestorAttachments refs authored deeper than the hierarchy, attached upwards
     * @param unmatchedRefs       refs with no anchor at all, dropped and reported for the author
     * @param questionsReplaced   active Master questions deactivated on exactly-matched controls
     * @param questionsKept       active questions left alone, on controls the pack is silent about
     * @param attributesWritten   {@code what_it_means} / {@code evidence} values stored
     */
    public record MergeCounts(int refRows,
                              int exactMatches,
                              int ancestorAttachments,
                              int unmatchedRefs,
                              int questionsReplaced,
                              int questionsKept,
                              int attributesWritten) {
    }

    public static FrameworkImportReportLine skipped(String sheetName,
                                                    String sourceFrameworkCode,
                                                    Long batchId,
                                                    int contentRows,
                                                    String message) {
        return new FrameworkImportReportLine(sheetName, sourceFrameworkCode, null, null,
                "SKIPPED", batchId, null, contentRows, 0, 0, 0, 0,
                new RoutingCounts(0, 0, 0, 0), null, List.of(), List.of(), message);
    }
}
