package com.a9.arbor.grc.catalog.web.dto;

import java.util.List;

public record FrameworkBundleStatusResponse(
        String directory,
        boolean directoryExists,
        List<String> modules,
        List<PackEntry> packs,
        List<String> skipped
) {
    public record PackEntry(
            String moduleFolder,
            String relativePath,
            String filename,
            boolean importable
    ) {
    }
}
