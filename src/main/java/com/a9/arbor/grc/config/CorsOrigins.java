package com.a9.arbor.grc.config;

import java.util.ArrayList;
import java.util.List;

/** Splits comma-separated CORS env values and drops localhost when the azure profile asks. */
final class CorsOrigins {

    private CorsOrigins() {
    }

    static List<String> split(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String item : raw) {
            if (item == null || item.isBlank()) {
                continue;
            }
            for (String part : item.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    out.add(trimmed);
                }
            }
        }
        return List.copyOf(out);
    }

    static List<String> effective(List<String> raw, boolean dropLocalDev) {
        List<String> split = split(raw);
        if (!dropLocalDev) {
            return split;
        }
        return split.stream().filter(origin -> !isLocalDev(origin)).toList();
    }

    static boolean isLocalDev(String origin) {
        if (origin == null) {
            return false;
        }
        String value = origin.trim().toLowerCase();
        return value.startsWith("http://localhost")
                || value.startsWith("https://localhost")
                || value.startsWith("http://127.0.0.1")
                || value.startsWith("https://127.0.0.1");
    }
}
