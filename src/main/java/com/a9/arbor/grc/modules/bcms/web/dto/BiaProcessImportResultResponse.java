package com.a9.arbor.grc.modules.bcms.web.dto;

import java.util.List;

/** Result of BIA process Excel import (valid rows created; failures reported). */
public record BiaProcessImportResultResponse(
        int createdCount,
        int failedCount,
        int ignoredCount,
        int skippedBlankCount,
        List<BiaProcessImportRowResult> rows
) {
    public record BiaProcessImportRowResult(
            int excelRow,
            String processCode,
            boolean success,
            Long processId,
            String completenessLabel,
            String error,
            boolean ignored
    ) {
        public static BiaProcessImportRowResult added(int excelRow, String processCode, Long processId,
                                                     String completenessLabel, String note) {
            return new BiaProcessImportRowResult(excelRow, processCode, true, processId, completenessLabel, note, false);
        }

        public static BiaProcessImportRowResult failed(int excelRow, String processCode, String error) {
            return new BiaProcessImportRowResult(excelRow, processCode, false, null, null, error, false);
        }

        public static BiaProcessImportRowResult ignored(int excelRow, String processCode, String message) {
            return new BiaProcessImportRowResult(excelRow, processCode, false, null, null, message, true);
        }
    }
}
