package com.a9.arbor.grc.catalog.service;

/**
 * Which module a consumer is seeding from the catalog.
 */
public enum ModuleRouting {
    SOA,
    GAP,
    AUDIT,
    RISK,
    /** No routing filter: activation / metadata callers. */
    ANY;

    public String emptyMessage(String frameworkCode) {
        String code = frameworkCode == null ? "this framework" : frameworkCode;
        return switch (this) {
            case SOA -> code + " has no nodes flagged for Statement of Applicability (in_soa)";
            case GAP -> code + " has no nodes flagged for Gap Assessment (in_gap_assessment)";
            case AUDIT -> code + " has no nodes flagged for Audit checklist (in_audit_checklist)";
            case RISK -> code + " has no nodes flagged for Risk register (in_risk_register)";
            case ANY -> code + " has no nodes to seed";
        };
    }
}
