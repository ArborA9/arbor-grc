package com.a9.arbor.grc.catalog.web.dto;

public record CrossFrameworkMappingEndpoint(
        long frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkVersion,
        long nodeId,
        String nodeCode,
        String nodeTitle
) {
}
