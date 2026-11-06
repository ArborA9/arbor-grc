package com.a9.arbor.grc.catalog.web.dto;

import java.util.List;

/**
 * Result of a Super Admin framework import (one or many frameworks from a single upload).
 *
 * <p>{@code report} carries the per-framework outcome: including sheets skipped and rows rejected -
 * and is empty for the legacy templates, which fail the whole upload on any bad row.
 */
public record FrameworkImportResultResponse(
        List<FrameworkDetailResponse> imported,
        List<FrameworkImportReportLine> report
) {
    public FrameworkImportResultResponse(List<FrameworkDetailResponse> imported) {
        this(imported, List.of());
    }
}
