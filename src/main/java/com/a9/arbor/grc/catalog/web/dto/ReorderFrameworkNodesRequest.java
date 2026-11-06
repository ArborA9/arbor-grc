package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Ordered node ids become displayOrder 1..N (Excel order). */
public record ReorderFrameworkNodesRequest(
        @NotEmpty List<Long> nodeIdsInOrder
) {
}
