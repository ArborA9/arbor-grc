package com.a9.arbor.grc.modules.capa.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectCapaRequest(@NotBlank String reason) {
}
