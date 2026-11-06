package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnAuditRequest(@NotBlank String reason) {
}
