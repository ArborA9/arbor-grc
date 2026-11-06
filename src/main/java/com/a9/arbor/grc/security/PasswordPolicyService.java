package com.a9.arbor.grc.security;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates local-account passwords. Relaxed mode (dev default) keeps min-length only so E2E
 * fixtures stay stable; strict mode (azure profile) adds complexity rules per §4.
 */
@Service
public class PasswordPolicyService {

    private final boolean strict;
    private final int minLength;

    public PasswordPolicyService(ArborSecurityProperties properties) {
        var pw = properties.getPassword();
        this.strict = pw.isStrict();
        this.minLength = pw.getMinLength();
    }

    public int minLength() {
        return minLength;
    }

    /** Upper case, lower case, and a digit are required. */
    public boolean mixedCaseRequired() {
        return strict;
    }

    /** A character that is not a letter, digit, or whitespace is required. */
    public boolean specialCharRequired() {
        return strict;
    }

    public static boolean isSpecial(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }

    public void assertValid(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < minLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + minLength + " characters");
        }
        if (!strict) {
            return;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char c : rawPassword.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (isSpecial(c)) {
                special = true;
            }
        }
        if (!upper || !lower || !digit || !special) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must include upper case, lower case, a number, and a special character");
        }
    }
}
