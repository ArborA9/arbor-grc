package com.a9.arbor.grc.catalog;

import java.util.regex.Pattern;

/** Strips a leftover {@code [E2E]} token from catalog titles. */
public final class E2eCatalogMarker {

    private static final Pattern MARKER = Pattern.compile("(?i)\\s*\\[E2E\\]");

    private E2eCatalogMarker() {
    }

    public static String strip(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return MARKER.matcher(text).replaceAll("").stripTrailing();
    }
}
