package com.a9.arbor.grc.catalog.excel;

import java.util.List;
import java.util.Set;

/**
 * What a reference pack wants written against one framework, decided by {@link ReferencePackMerger}
 * before anything touches the catalog.
 *
 * @param replacedCodes  nodes the pack matched exactly — their active Master questions are
 *                       deactivated, since the pack wins per control
 * @param attachedCodes  nodes that only received questions from a deeper ref — their Master
 *                       questions survive, so the new ones are numbered after them
 * @param staging        one row per source row plus banners, for {@code framework_import_row}
 * @param errors         dropped rows, reported rather than silently lost
 * @param info           every ancestor attachment, so a coarse join is visible in the report
 */
public record ReferenceMergePlan(
        List<QuestionWrite> questions,
        List<AttributeWrite> attributes,
        Set<String> replacedCodes,
        Set<String> attachedCodes,
        List<ImportStagingRow> staging,
        List<ImportRowError> errors,
        List<String> info,
        Counts counts
) {

    /**
     * @param nodeCode  the node the question lands on
     * @param sourceRef the ref the client authored it against, null when that is {@code nodeCode}
     * @param ordinal   1-based within {@code nodeCode} across the whole pack; the persisted
     *                  {@code question_number} adds an offset for questions kept from the Master
     */
    public record QuestionWrite(String nodeCode, String sourceRef, int ordinal, String questionText) {
    }

    public record AttributeWrite(String nodeCode, String attrKey, String attrValue) {
    }

    public record Counts(int refRows, int exactMatches, int ancestorAttachments, int unmatchedRefs) {
    }
}
