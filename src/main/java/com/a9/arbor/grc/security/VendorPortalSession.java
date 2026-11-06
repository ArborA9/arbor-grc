package com.a9.arbor.grc.security;

/**
 * Invite id for the current TPRM vendor JWT ({@link JwtService#CLAIM_VENDOR_TOKEN_ID}).
 * Cleared at the end of each request with {@link TenantContext}.
 */
public final class VendorPortalSession {

    private static final ThreadLocal<Long> TOKEN_ID = new ThreadLocal<>();

    private VendorPortalSession() {
    }

    public static void setTokenId(Long vendorTokenId) {
        if (vendorTokenId == null) {
            TOKEN_ID.remove();
        } else {
            TOKEN_ID.set(vendorTokenId);
        }
    }

    public static Long tokenId() {
        return TOKEN_ID.get();
    }

    public static void clear() {
        TOKEN_ID.remove();
    }
}
