package com.a9.arbor.grc.modules.iam.service;

import java.time.Instant;

/**
 * Decides what a presented refresh token means.
 * A second tab that retries within {@link #GRACE_SECONDS} is a race, not theft.
 * A replay after that, of a token we already rotated, revokes every refresh token for the user.
 */
public final class RefreshReuse {

    public static final int GRACE_SECONDS = 60;
    public static final String ROTATED = "ROTATED";
    public static final String LOGOUT = "LOGOUT";

    public enum Decision {
        /** Token is live. Revoke it and issue a new one. */
        ROTATE,
        /** Token was rotated moments ago. Issue another token. Leave the other sessions. */
        GRACE_REISSUE,
        /** Token was rotated and is being used again. Revoke every refresh token for this user. */
        REUSE_REVOKE_ALL,
        /** Missing, expired, or logged out. Do not revoke other sessions. */
        REJECT
    }

    private RefreshReuse() {
    }

    public static Decision decide(Instant now, Instant expiresAt, Instant revokedAt, String revokedReason) {
        if (revokedAt == null) {
            if (expiresAt != null && !expiresAt.isAfter(now)) {
                return Decision.REJECT;
            }
            return Decision.ROTATE;
        }
        if (!ROTATED.equals(revokedReason)) {
            return Decision.REJECT;
        }
        if (revokedAt.plusSeconds(GRACE_SECONDS).isAfter(now)) {
            return Decision.GRACE_REISSUE;
        }
        return Decision.REUSE_REVOKE_ALL;
    }
}
