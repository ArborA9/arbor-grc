package com.a9.arbor.grc.modules.evidence.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Extension ↔ declared Content-Type check (F6 / J3 polish). Allows blank / octet-stream. */
final class EvidenceContentTypeSupport {

    private static final Map<String, Set<String>> EXT_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"));

    private EvidenceContentTypeSupport() {
    }

    static void assertCompatible(String extension, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        String ct = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (ct.isEmpty() || "application/octet-stream".equals(ct)) {
            return;
        }
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        Set<String> allowed = EXT_TYPES.get(ext);
        if (allowed != null && !allowed.contains(ct)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Content-Type " + ct + " does not match file extension ." + ext);
        }
    }
}
