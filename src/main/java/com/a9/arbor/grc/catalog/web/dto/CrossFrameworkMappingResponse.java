package com.a9.arbor.grc.catalog.web.dto;

public record CrossFrameworkMappingResponse(
        long id,
        String mappingType,
        String justification,
        CrossFrameworkMappingEndpoint peer,
        String direction
) {
}
