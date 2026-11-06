package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.a9.arbor.grc.catalog.excel.GrcChecklistExcelParser;

public record FrameworkTypeResponse(
        String code,
        String name,
        String moduleCode,
        String defaultVersion,
        boolean dedicatedParserExists,
        boolean sampleAvailable
) {
    public static FrameworkTypeResponse from(FrameworkTypeCode type) {
        return new FrameworkTypeResponse(
                type.name(),
                type.displayName(),
                type.moduleCode().name(),
                type.defaultVersion(),
                GrcChecklistExcelParser.supportsType(type),
                true);
    }
}
