package com.a9.arbor.grc.modules.risk.domain;

/** UAE IA control priority (P1–P4). Auto-populated from framework node for IS risks. */
public enum UaeIaPriority {
    P1,
    P2,
    P3,
    P4;

    public static UaeIaPriority fromNodePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().toUpperCase().replace(" ", "");
        if (t.startsWith("P1") || t.equals("1") || t.contains("PRIORITY1")) {
            return P1;
        }
        if (t.startsWith("P2") || t.equals("2") || t.contains("PRIORITY2")) {
            return P2;
        }
        if (t.startsWith("P3") || t.equals("3") || t.contains("PRIORITY3")) {
            return P3;
        }
        if (t.startsWith("P4") || t.equals("4") || t.contains("PRIORITY4")) {
            return P4;
        }
        try {
            return UaeIaPriority.valueOf(t);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
