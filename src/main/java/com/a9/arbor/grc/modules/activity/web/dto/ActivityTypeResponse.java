package com.a9.arbor.grc.modules.activity.web.dto;

public record ActivityTypeResponse(
        String code,
        String label,
        String moduleCode,
        String description,
        /** false = registered for future (e.g. BIA stub). */
        boolean implemented,
        String navPath
) {
}
