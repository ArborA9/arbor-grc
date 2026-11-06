package com.a9.arbor.grc.modules.soa.web.dto;

import com.a9.arbor.grc.modules.soa.domain.SoaApplicability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bulk-set applicability on SoA items.
 * <ul>
 *   <li>UNANSWERED: rows with no applicability yet</li>
 *   <li>ALL: every control (overwrites)</li>
 *   <li>SELECTED: only {@code itemIds}</li>
 * </ul>
 * Justification is required when marking {@code NOT_APPLICABLE}; optional for {@code APPLICABLE}.
 */
public record BulkUpdateSoaItemsRequest(
        @NotNull SoaApplicability applicability,
        @Size(max = 8000) String justification,
        @NotNull BulkSoaScope scope,
        List<Long> itemIds
) {
    public enum BulkSoaScope {
        UNANSWERED,
        ALL,
        SELECTED
    }
}
