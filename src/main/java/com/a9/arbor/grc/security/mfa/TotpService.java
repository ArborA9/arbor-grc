package com.a9.arbor.grc.security.mfa;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * RFC 6238 (TOTP) on top of RFC 4226 (HOTP), implemented directly rather than pulling in a
 * dependency: this is ~80 lines of well-specified, testable crypto. HMAC-SHA1 / 6 digits / 30s
 * step, matching every mainstream authenticator app (Google/Microsoft Authenticator, Authy, etc).
 */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20; // 160-bit, matches RFC 4226 recommendation
    private static final int DIGITS = 6;
    private static final int STEP_SECONDS = 30;
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final SecureRandom random = new SecureRandom();

    /** Generates a new random Base32-encoded secret suitable for an authenticator app. */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** {@code otpauth://} URI for QR-code enrollment (issuer + account label, no external calls). */
    public String buildAuthUri(String secretBase32, String accountEmail, String issuer) {
        String label = URLEncoder.encode(issuer + ":" + accountEmail, StandardCharsets.UTF_8).replace("+", "%20");
        String issuerEnc = URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + secretBase32 + "&issuer=" + issuerEnc
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    /** Verifies a submitted 6-digit code, tolerating +/-1 step (30s) of clock drift. */
    public boolean verifyCode(String secretBase32, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentStep = Instant.now().getEpochSecond() / STEP_SECONDS;
        for (long drift = -1; drift <= 1; drift++) {
            if (code.equals(generateCode(secretBase32, currentStep + drift))) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String secretBase32, long counter) {
        byte[] key = base32Decode(secretBase32);
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte) (counter & 0xFF);
            counter >>= 8;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hash = mac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute TOTP code", e);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1F]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String base32) {
        String cleaned = base32.trim().toUpperCase().replace("=", "");
        int outputLength = cleaned.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : cleaned.toCharArray()) {
            int value = new String(BASE32_ALPHABET).indexOf(c);
            if (value < 0) {
                continue;
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
