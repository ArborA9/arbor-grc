package com.a9.arbor.grc.modules.dashboard.web.dto;

/**
 * One dashboard widget (req §19.1/19.2). {@code value} is pre-formatted (count, percentage, etc.)
 * so the SPA never has to reason about units. {@code navTarget} matches an existing client nav id.
 */
public record DashboardWidget(
        String id,
        String title,
        String module,
        String value,
        String subtitle,
        String tone,
        String navTarget,
        String emptyMessage) {

    public static DashboardWidget of(String id, String title, String module, long value,
                                     String subtitle, String tone, String navTarget, String emptyMessage) {
        return new DashboardWidget(id, title, module, String.valueOf(value), subtitle, tone, navTarget, emptyMessage);
    }
}
