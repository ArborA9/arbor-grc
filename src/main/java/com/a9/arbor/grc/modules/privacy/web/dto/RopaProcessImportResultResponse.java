package com.a9.arbor.grc.modules.privacy.web.dto;

import java.util.List;

/** Result of Champion ROPA process Excel import (rows created as drafts). */
public record RopaProcessImportResultResponse(
        int createdCount,
        int failedCount,
        int ignoredCount,
        int skippedBlankCount,
        List<RopaProcessImportRowResult> rows
) {
    public record RopaProcessImportRowResult(
            int excelRow,
            String activityName,
            boolean success,
            Long processId,
            String completenessLabel,
            String error,
            boolean ignored
    ) {
        public static RopaProcessImportRowResult added(int excelRow, String activityName, Long processId) {
            return new RopaProcessImportRowResult(
                    excelRow, activityName, true, processId, "Draft", null, false);
        }

        public static RopaProcessImportRowResult failed(int excelRow, String activityName, String error) {
            return new RopaProcessImportRowResult(excelRow, activityName, false, null, null, error, false);
        }

        public static RopaProcessImportRowResult ignored(int excelRow, String activityName, String message) {
            return new RopaProcessImportRowResult(excelRow, activityName, false, null, null, message, true);
        }
    }
}
