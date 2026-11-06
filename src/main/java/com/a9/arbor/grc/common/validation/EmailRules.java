package com.a9.arbor.grc.common.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

/** Shared email validation: keep in sync with {@code frontend/src/validation/email.ts}. */
public final class EmailRules {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailRules() {
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && EMAIL.matcher(trimmed).matches();
    }

    /** Required email: throws 400 when blank or malformed. */
    public static void requireValid(String value, String fieldLabel) {
        if (!isValid(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " must be a valid email address");
        }
    }

    /** Optional email: null/blank allowed; non-blank must be valid. */
    public static void requireValidIfPresent(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!isValid(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " must be a valid email address");
        }
    }
}
