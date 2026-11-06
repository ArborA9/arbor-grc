package com.a9.arbor.grc.modules.risk.web.dto;

/** Contributor eligible for assignment to a risk's division/department. */
public record EligibleContributorResponse(
        Long id,
        String email,
        String fullName
) {
}
