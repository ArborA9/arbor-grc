package com.a9.arbor.grc.catalog.web.dto;

import java.util.List;

public record FrameworkBundleImportResultResponse(
        String scope,
        String moduleFolder,
        String relativePath,
        boolean dryRun,
        int importedFileCount,
        int skippedFileCount,
        int errorFileCount,
        List<FrameworkBundleFileResult> files
) {
}
