package com.a9.arbor.grc.modules.evidence.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/** Magic-byte sniffing for allowed evidence extensions (P1-7). */
final class EvidenceMagicBytesSupport {

    private EvidenceMagicBytesSupport() {
    }

    static void assertMatchesExtension(String extension, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!matches(ext, bytes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File content does not match extension ." + ext);
        }
    }

    private static boolean matches(String ext, byte[] b) {
        return switch (ext) {
            case "pdf" -> startsWith(b, new byte[] {0x25, 0x50, 0x44, 0x46}); // %PDF
            case "png" -> startsWith(b, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
            case "jpg", "jpeg" -> b.length >= 3 && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF;
            case "docx", "xlsx" -> startsWith(b, new byte[] {0x50, 0x4B, 0x03, 0x04}); // ZIP / OOXML
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
