package com.a9.arbor.grc.modules.iam.web.dto;

import java.time.Instant;

/**
 * What a set-password link is, checked without using it up.
 *
 * @param state VALID, EXPIRED, USED (spent or replaced by a newer email), ACTIVATED (activation link,
 *              account already has a password), or INVALID (unknown link, workspace, or account)
 * @param purpose ACTIVATION or RESET; null when INVALID
 */
public record PasswordLinkStatusResponse(
        String state,
        String purpose,
        String maskedEmail,
        String workspaceId,
        String organisation,
        Instant expiresAt,
        int passwordMinLength,
        boolean passwordMixedCase,
        boolean passwordSpecialChar
) {
}
