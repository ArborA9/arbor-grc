package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveBiaImpactRequest(@NotNull @Valid List<BiaImpactCellRequest> cells) {
}
