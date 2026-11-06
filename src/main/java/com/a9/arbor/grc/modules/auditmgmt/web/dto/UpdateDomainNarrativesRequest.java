package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateDomainNarrativesRequest(
        @NotNull List<DomainNarrativeUpdate> narratives
) {
    public record DomainNarrativeUpdate(
            String code,
            String narrative
    ) {
    }
}
