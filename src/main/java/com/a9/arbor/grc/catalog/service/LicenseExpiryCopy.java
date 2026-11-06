package com.a9.arbor.grc.catalog.service;

/** User-facing copy when a licensed module has passed its last inclusive day. */
public final class LicenseExpiryCopy {

    public static final String CODE = "LICENSE_EXPIRED";

    private LicenseExpiryCopy() {
    }

    public static String writeForbidden(String moduleCode) {
        if ("AUDIT".equals(moduleCode)) {
            return "Audit is view-only because every licensed module has expired. Contact Arbor to extend a licence.";
        }
        String code = moduleCode == null || moduleCode.isBlank() ? "This module" : moduleCode;
        return code + " is view-only because its licence has expired. Other licensed modules stay fully usable. Contact Arbor to extend "
                + code + ".";
    }

    /** Audit comes with any paid module, so "not licensed" means no paid module is on. */
    public static String notLicensed(String moduleNames) {
        if ("AUDIT".equals(moduleNames)) {
            return "Audit is available with any licensed module. Contact Arbor to license a module.";
        }
        return "Module " + moduleNames + " is not licensed for this tenant";
    }

    public static String bannerMessage(String names, String date) {
        String who = names == null || names.isBlank() ? "A licensed module" : names;
        String when = date == null ? "" : " (expired " + date + ")";
        return who + " is view-only" + when
                + ". You can still work in other licensed modules. Contact your Arbor account team to extend the expired licence.";
    }
}
