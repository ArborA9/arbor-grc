package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;

import java.util.function.Predicate;

/**
 * Seed-time routing overrides that must not rewrite catalog flags.
 * ISO 22301: Gap scope follows audit checklist leaves (F7).
 */
public final class CatalogRoutingOverrides {

    private CatalogRoutingOverrides() {
    }

    public static Predicate<FrameworkNodeResponse> effective(String frameworkCode, ModuleRouting routing) {
        boolean iso22301 = frameworkCode != null
                && frameworkCode.toUpperCase().contains("22301");
        return node -> switch (routing) {
            case ANY -> true;
            case SOA -> node.inSoa();
            case GAP -> iso22301 ? node.inAuditChecklist() : node.inGapAssessment();
            case AUDIT -> node.inAuditChecklist();
            case RISK -> node.inRiskRegister();
        };
    }
}
