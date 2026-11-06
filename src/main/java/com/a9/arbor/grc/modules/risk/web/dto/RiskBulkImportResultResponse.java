package com.a9.arbor.grc.modules.risk.web.dto;

import java.util.List;

/** Result of bulk IS/ERM risk Excel import (valid rows committed; failures reported). */
public record RiskBulkImportResultResponse(
        int createdCount,
        int failedCount,
        int skippedBlankCount,
        int ignoredCount,
        List<RiskBulkImportRowResult> rows
) {
    public record RiskBulkImportRowResult(
            int excelRow,
            boolean success,
            boolean ignored,
            Long riskId,
            String displayId,
            String error
    ) {
    }
}
