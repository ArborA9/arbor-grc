package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateExternalAccessInviteRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @Positive Integer expiryDays
) {
}
