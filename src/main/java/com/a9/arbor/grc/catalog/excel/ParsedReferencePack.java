package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;

import java.util.List;

/**
 * Parse output for a client reference pack (layout S4).
 *
 * <p>Purely what the workbook says. Nothing here is resolved against the catalog: matching refs to
 * nodes is {@link ReferencePackMerger}'s job, because it needs the codes the Master import created.
 */
public record ParsedReferencePack(
        List<Sheet> sheets,
        List<SkippedSheet> skipped,
        List<DomainObjective> domainObjectives
) {

    public ParsedReferencePack(List<Sheet> sheets, List<SkippedSheet> skipped) {
        this(sheets, skipped, List.of());
    }

    public ParsedReferencePack {
        sheets = List.copyOf(sheets);
        skipped = List.copyOf(skipped);
        domainObjectives = domainObjectives == null ? List.of() : List.copyOf(domainObjectives);
    }

    /**
     * UAE IA Domain Objectives sheet — guidance text keyed by domain / sub-domain code.
     * Not assessable rows; attached as {@code domain_objective} attrs on matching nodes.
     *
     * @param code      Domain ({@code M1}) or Sub-Domain ({@code M1.1})
     * @param title     Title column
     * @param objective Objective column (tooltip body)
     */
    public record DomainObjective(String code, String title, String objective) {
    }

    /**
     * One enrichable sheet. Two sheets can target the same framework — the ISO 27001 pack splits
     * clauses 4–10 and Annex A — so the merge runs per framework, not per sheet.
     */
    public record Sheet(
            String sheetName,
            FrameworkTypeCode type,
            List<ReferenceRow> rows,
            List<Banner> banners,
            List<ImportRowError> errors,
            int contentRows
    ) {

        /** Banners are staged verbatim whether or not a hierarchy builder reads their text. */
        public List<ImportStagingRow> bannerStaging() {
            return banners.stream().map(Banner::staging).toList();
        }
    }

    /**
     * A single-cell section row. Enrichment only stages these, but {@link PackFrameworkBuilder} reads
     * the text: in a pack-created framework the banners are the <em>only</em> source of titles for
     * the domain and section nodes above the authored refs.
     *
     * @param text the first populated cell, verbatim
     */
    public record Banner(int excelRow, String text, ImportStagingRow staging) {
    }

    /**
     * @param ref           the clause / control ref as authored, e.g. {@code 4.2.1}, {@code A.5.1},
     *                      {@code CLD.6.3.1}
     * @param definition    the verbatim requirement / statement / legal text, or null. Unused by
     *                      enrichment, where the Master stays authoritative for node text; it becomes
     *                      {@code definition_text} when the pack creates the framework
     * @param priority      UAE IA {@code P1}..{@code P4}, or null where the pack has no such column
     * @param applicability UAE IA applicability, or null. Not derivable from priority — P2 splits
     *                      across both values — so it is carried rather than computed
     * @param questions     the row's {@code Q1..Qn} / {@code Question 1..n} cells, blanks dropped,
     *                      left-to-right
     * @param whatItMeans   the "What It Means" column, or null
     * @param evidence      the "Evidence" column, or null
     * @param rawJson       every column on the row, for {@code framework_import_row}
     */
    public record ReferenceRow(
            int excelRow,
            String ref,
            String title,
            String definition,
            String priority,
            String applicability,
            List<String> questions,
            String whatItMeans,
            String evidence,
            String rawJson,
            /** Optional parent code for non-dotted hierarchies (SAMA subdomain). */
            String structuralParent
    ) {
        public ReferenceRow(int excelRow, String ref, String title, String definition, String priority,
                            String applicability, List<String> questions, String whatItMeans,
                            String evidence, String rawJson) {
            this(excelRow, ref, title, definition, priority, applicability, questions, whatItMeans,
                    evidence, rawJson, null);
        }
    }

    public record SkippedSheet(
            String sheetName,
            String reason,
            List<ImportStagingRow> staging
    ) {
    }
}
