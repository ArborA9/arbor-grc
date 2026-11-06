package com.a9.arbor.grc.common.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Safe Content-Disposition headers (RFC 5987 filename* + ASCII fallback). */
public final class ContentDispositionSupport {

    private ContentDispositionSupport() {
    }

    public static String attachment(String filename) {
        String safe = filename == null || filename.isBlank() ? "download" : filename.trim();
        String ascii = safe.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (ascii.isBlank()) {
            ascii = "download";
        }
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
