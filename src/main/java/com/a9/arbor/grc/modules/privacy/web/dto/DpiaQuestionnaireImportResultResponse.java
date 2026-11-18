package com.a9.arbor.grc.modules.privacy.web.dto;

import java.util.List;

public record DpiaQuestionnaireImportResultResponse(
        int createdCount,
        int failedCount,
        int ignoredCount,
        List<RowResult> rows
) {
    public record RowResult(int excelRow, String code, boolean success, String error, boolean ignored) {
        public static RowResult added(int excelRow, String code) {
            return new RowResult(excelRow, code, true, null, false);
        }

        public static RowResult failed(int excelRow, String code, String error) {
            return new RowResult(excelRow, code, false, error, false);
        }

        public static RowResult ignored(int excelRow, String code, String message) {
            return new RowResult(excelRow, code, false, message, true);
        }
    }
}
