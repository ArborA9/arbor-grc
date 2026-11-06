package com.a9.arbor.grc.catalog.web.dto;

/**
 * Expired-module banner for the modules that have lapsed. Live modules stay writable.
 */
public record LicenseBannerResponse(
        String key,
        String tone,
        String title,
        String message,
        boolean dismissed
) {
}
