package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.a9.arbor.grc.catalog.domain.ModuleCode;

import java.util.List;

/**
 * Parse output for the Master Import workbook. One {@link FrameworkSheet} per
 * {@code framework_code} found (a sheet may hold more than one, as {@code 15 UAE_Template} does),
 * plus the sheets deliberately not imported.
 */
public record ParsedMasterWorkbook(
        List<FrameworkSheet> frameworks,
        List<SkippedSheet> skipped
) {

    /**
     * @param rows        ancestors and leaves, ready for {@code persistImport}
     * @param staging     one entry per source row across both the framework and question sheets
     * @param errors      per-row failures; the offending rows are excluded from {@code rows} /
     *                    {@code questions} rather than aborting the framework
     * @param info        report lines for transformations applied, e.g. codes taken from
     *                    {@code official_ref}
     * @param contentRows source data rows, excluding headers and single-cell banner rows
     */
    public record FrameworkSheet(
            String sheetName,
            String questionSheetName,
            String sourceFrameworkCode,
            FrameworkTypeCode type,
            String version,
            ModuleCode moduleCode,
            List<ParsedFrameworkRow> rows,
            List<Question> questions,
            List<ImportStagingRow> staging,
            List<ImportRowError> errors,
            List<String> info,
            int contentRows
    ) {
        public long leafCount() {
            return rows.stream().filter(ParsedFrameworkRow::routedToAnyModule).count();
        }
    }

    public record Question(
            int excelRow,
            String controlCode,
            String questionNumber,
            String questionType,
            String questionText,
            String answerOptions,
            String evidenceGuidance,
            boolean mandatory,
            int displayOrder
    ) {
    }

    public record SkippedSheet(
            String sheetName,
            String sourceFrameworkCode,
            String reason,
            List<ImportStagingRow> staging
    ) {
    }
}
