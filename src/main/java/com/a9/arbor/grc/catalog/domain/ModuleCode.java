package com.a9.arbor.grc.catalog.domain;

/**
 * Commercially licensed feature modules (req TENANT_MODULES). Platform always-on surfaces
 * (Dashboard, Users/Settings, Evidence when built) are not represented here.
 */
public enum ModuleCode {
    ISMS,
    ERM,
    BCMS,
    PRIVACY,
    ITSM,
    AIMS,
    AUDIT,
    /** Kept for stored rows. Not part of this delivery: hidden from licence lists and the product. */
    CARF,
    /** Quality Management (ISO 9001): Framework Import V2. */
    QMS,
    /** Occupational Health & Safety (ISO 45001): Framework Import V2. */
    OHSMS,
    /** SAMA Cyber Security Framework: Framework Import V2. */
    SAMA;

    /** False for modules that exist in the enum but are not sold in this delivery. */
    public boolean delivered() {
        return this != CARF;
    }
}
