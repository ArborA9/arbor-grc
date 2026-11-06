package com.a9.arbor.grc.modules.bcms.web.dto;

/** One Dependency - Asset Type row. {@code type} is the lookup code. */
public record BiaAssetDependency(
        String type,
        String detail
) {
}
