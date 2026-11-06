package com.a9.arbor.grc.catalog.web.dto;

import java.util.List;

public record FrameworkBundleFileResult(
        int step,
        String moduleFolder,
        String relativePath,
        String filename,
        String status,
        String message,
        FrameworkImportResultResponse importResult,
        List<String> errors
) {
    public static FrameworkBundleFileResult skipped(int step, String moduleFolder,
                                                    String relativePath, String filename,
                                                    String message) {
        return new FrameworkBundleFileResult(step, moduleFolder, relativePath, filename,
                "SKIPPED", message, null, List.of());
    }
}
