package com.a9.arbor.grc.modules.tprm.domain;

public enum TprmAnswer {
    YES,
    PARTIAL,
    NO,
    NA;

    public String label() {
        return switch (this) {
            case YES -> "Yes";
            case PARTIAL -> "Partial";
            case NO -> "No";
            case NA -> "N/A";
        };
    }

    public static TprmAnswer parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String n = raw.trim().toUpperCase().replace(" ", "_").replace("/", "");
        return switch (n) {
            case "YES", "Y" -> YES;
            case "PARTIAL", "PARTIALLY" -> PARTIAL;
            case "NO", "N" -> NO;
            case "NA", "NAA", "NOT_APPLICABLE", "NOTAPPLICABLE" -> NA;
            default -> throw new IllegalArgumentException("Unknown vendor answer: " + raw);
        };
    }
}
