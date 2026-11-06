package com.a9.arbor.grc.catalog.web.dto;

/** Optional body for {@code POST /api/frameworks/{id}/activate}. */
public record ActivateFrameworkRequest(Boolean includeCloud) {
}
