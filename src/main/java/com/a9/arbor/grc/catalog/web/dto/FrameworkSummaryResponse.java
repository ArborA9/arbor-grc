package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.Framework;

import java.time.Instant;

public record FrameworkSummaryResponse(
        Long id,
        String code,
        String name,
        String version,
        String moduleCode,
        String status,
        int controlCount,
        String importedBy,
        Instant importedAt,
        String validatedBy,
        Instant validatedAt,
        String publishedBy,
        Instant publishedAt,
        String sourceFilename,
        Boolean activated,
        Instant activatedAt,
        FrameworkRoutingCounts routing,
        /** Tenant activation scope: null when not activated / not tenant context. */
        Boolean includeCloud,
        /** Catalog has CLOUD record-type nodes (ISO 27017 sheet). */
        Boolean hasCloud,
        /** Doc 02 T-11: CLAUSE record-type nodes (null when not computed). */
        Integer clauseCount,
        /** Doc 02 T-11: ANNEX_CONTROL + CONTROL + CLOUD nodes (null when not computed). */
        Integer annexControlCount
) {
    public static FrameworkSummaryResponse from(Framework f) {
        return from(f, null, null, null, null, null, null, null);
    }

    public static FrameworkSummaryResponse from(Framework f, Boolean activated, Instant activatedAt) {
        return from(f, activated, activatedAt, null, null, null, null, null);
    }

    public static FrameworkSummaryResponse from(Framework f, Boolean activated, Instant activatedAt,
                                                FrameworkRoutingCounts routing) {
        return from(f, activated, activatedAt, routing, null, null, null, null);
    }

    public static FrameworkSummaryResponse from(Framework f, Boolean activated, Instant activatedAt,
                                                FrameworkRoutingCounts routing,
                                                Boolean includeCloud,
                                                Boolean hasCloud) {
        return from(f, activated, activatedAt, routing, includeCloud, hasCloud, null, null);
    }

    public static FrameworkSummaryResponse from(Framework f, Boolean activated, Instant activatedAt,
                                                FrameworkRoutingCounts routing,
                                                Boolean includeCloud,
                                                Boolean hasCloud,
                                                Integer clauseCount,
                                                Integer annexControlCount) {
        return new FrameworkSummaryResponse(
                f.getId(),
                f.getCode(),
                f.getName(),
                f.getVersion(),
                f.getModuleCode().name(),
                f.getStatus().name(),
                f.getControlCount(),
                f.getImportedBy(),
                f.getImportedAt(),
                f.getValidatedBy(),
                f.getValidatedAt(),
                f.getPublishedBy(),
                f.getPublishedAt(),
                f.getSourceFilename(),
                activated,
                activatedAt,
                routing,
                includeCloud,
                hasCloud,
                clauseCount,
                annexControlCount);
    }
}
