package com.a9.arbor.grc.modules.project.web.dto;

public record ProjectFrameworkResponse(
        Long frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkVersion
) {
}
