package com.a9.arbor.grc.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Secure token / OTP generation and SHA-256 hashing for one-shot access links. */
public final class SecureTokenUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenUtils() {
    }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String randomOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /**
     * When outbound email is not actually sending, a stable OTP so admin/vendor
     * testing can proceed. Switch back to {@link #randomOtp()} once SMTP/ACS is live.
     */
    /** Fixed until outbound email is live for vendor invites. */
    public static final String LOCAL_PREVIEW_OTP = "1111";

    public static String inviteOtp(boolean emailLive) {
        return emailLive ? randomOtp() : LOCAL_PREVIEW_OTP;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
