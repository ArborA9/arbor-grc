package com.a9.arbor.grc.modules.license.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DismissLicenseBannerRequest(@NotBlank String dismissKey) {
}
