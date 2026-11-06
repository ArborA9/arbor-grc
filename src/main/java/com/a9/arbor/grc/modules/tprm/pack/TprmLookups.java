package com.a9.arbor.grc.modules.tprm.pack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pack Lookups + Calculation Reference. Scores are not hard-coded in the UI. */
public final class TprmLookups {

    private TprmLookups() {
    }

    public static final Map<String, Integer> F1 = scores(
            "Secret", 4,
            "Sensitive", 3,
            "Confidential", 2,
            "Open", 1);
    public static final Map<String, Integer> F2 = scores(
            "Privileged or administrative access", 4,
            "Standard named user access", 3,
            "Read-only or view access, or physical access to a critical area", 2,
            "No access of any kind", 1);
    public static final Map<String, Integer> F3 = scores(
            "A critical service stops, with no workaround", 4,
            "A critical service degrades, a manual workaround exists", 3,
            "An important but non-critical service is affected", 2,
            "Minimal operational effect", 1);
    public static final Map<String, Integer> F4 = scores(
            "Vendor-owned infrastructure, worked remotely / off site", 4,
            "Vendor-owned laptops or devices, worked on Organization premises", 3,
            "Organization-provided assets, worked remotely / off site", 2,
            "Organization-provided assets, worked on Organization premises", 1);
    public static final Map<String, Integer> F5 = scores(
            "Large volumes, or special-category data", 4,
            "Routine personal data as part of the service", 3,
            "Incidental exposure only", 2,
            "No personal data", 1);
    public static final Map<String, Integer> F6 = scores(
            "Sole source, replacement would take more than six months", 4,
            "Few alternatives, replacement would take three to six months", 3,
            "Several alternatives, replacement within three months", 2,
            "Readily replaceable, commodity service", 1);

    public static List<String> options(Map<String, Integer> factor) {
        return factor.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    public static int score(Map<String, Integer> factor, String option) {
        if (option == null || option.isBlank()) {
            throw new IllegalArgumentException("Factor option is required");
        }
        Integer s = factor.get(option);
        if (s == null) {
            throw new IllegalArgumentException("Unknown factor option: " + option);
        }
        return s;
    }

    public static String tierForAdjusted(int adjusted) {
        if (adjusted >= 20) {
            return "TIER_1";
        }
        if (adjusted >= 16) {
            return "TIER_2";
        }
        if (adjusted >= 11) {
            return "TIER_3";
        }
        return "TIER_4";
    }

    public static int tierNumber(String tier) {
        return switch (tier) {
            case "TIER_1" -> 1;
            case "TIER_2" -> 2;
            case "TIER_3" -> 3;
            case "TIER_4" -> 4;
            default -> throw new IllegalArgumentException("Unknown tier: " + tier);
        };
    }

    public static String nextTierUp(String tier) {
        return switch (tier) {
            case "TIER_4" -> "TIER_3";
            case "TIER_3" -> "TIER_2";
            case "TIER_2" -> "TIER_1";
            default -> "TIER_1";
        };
    }

    public static boolean critical(String tier) {
        return "TIER_1".equals(tier) || "TIER_2".equals(tier);
    }

    public static double targetForTier(String tier) {
        return switch (tier) {
            case "TIER_1" -> 0.9;
            case "TIER_2" -> 0.8;
            case "TIER_3" -> 0.7;
            default -> 0.6;
        };
    }

    public static int monthsForTier(String tier) {
        return switch (tier) {
            case "TIER_1" -> 12;
            case "TIER_2" -> 18;
            case "TIER_3" -> 24;
            default -> 36;
        };
    }

    private static Map<String, Integer> scores(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
