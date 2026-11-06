package com.a9.arbor.grc.security.mfa;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM at-rest encryption for TOTP secrets (Path C B4). Uses a dedicated key when configured
 * ({@code arbor.security.mfa.encryption-key}, Base64 32 bytes); otherwise derives one from the JWT
 * signing secret so local/dev environments work without extra setup: production should always
 * supply a distinct Key Vault-backed key.
 */
@Component
public class TotpSecretCipher {

    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public TotpSecretCipher(ArborSecurityProperties properties) throws GeneralSecurityException {
        this.key = new SecretKeySpec(resolveKeyBytes(properties), "AES");
    }

    private static byte[] resolveKeyBytes(ArborSecurityProperties properties) throws GeneralSecurityException {
        String configured = properties.getMfa().getEncryptionKey();
        if (configured != null && !configured.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(configured.trim());
            if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
                throw new IllegalStateException(
                        "arbor.security.mfa.encryption-key must decode to 16/24/32 bytes");
            }
            return decoded;
        }
        String jwtSecret = properties.getJwt().getSecret();
        return MessageDigest.getInstance("SHA-256").digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt TOTP secret", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_BYTES);
            byte[] cipherText = new byte[combined.length - GCM_IV_BYTES];
            System.arraycopy(combined, GCM_IV_BYTES, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt TOTP secret", e);
        }
    }
}
