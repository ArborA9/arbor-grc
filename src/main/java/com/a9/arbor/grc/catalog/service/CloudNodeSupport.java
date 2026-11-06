package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;

import java.util.Locale;
import java.util.Map;

/**
 * Cloud controls live on an ISO 27001 sheet (not a licensed module). Framework Import V2 Phase 4.
 */
public final class CloudNodeSupport {

    private CloudNodeSupport() {
    }

    public static boolean isCloud(FrameworkNodeResponse node) {
        if (node == null) {
            return false;
        }
        if (eq(node.recordType(), "CLOUD")) {
            return true;
        }
        Map<String, String> attrs = node.attributes();
        if (attrs != null) {
            if (eq(attrs.get("section_type"), "CLOUD")) {
                return true;
            }
            String tab = attrs.get("sheet_tab");
            if (tab != null) {
                String s = tab.toLowerCase(Locale.ROOT);
                if (s.contains("27017") || s.contains("cloud")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Clauses and Excel "Always" applicability: SoA cannot set N/A. */
    public static boolean isApplicabilityLocked(FrameworkNodeResponse node) {
        if (node == null) {
            return false;
        }
        if (eq(node.recordType(), "CLAUSE") || eq(attr(node, "section_type"), "CLAUSE")) {
            return true;
        }
        return "ALWAYS".equalsIgnoreCase(attr(node, "default_applicability"));
    }

    public static String attr(FrameworkNodeResponse node, String key) {
        if (node == null || node.attributes() == null || key == null) {
            return null;
        }
        String v = node.attributes().get(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}
