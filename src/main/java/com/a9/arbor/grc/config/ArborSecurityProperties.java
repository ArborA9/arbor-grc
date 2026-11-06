package com.a9.arbor.grc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Security / JWT configuration. */
@Data
@ConfigurationProperties(prefix = "arbor.security")
public class ArborSecurityProperties {

    private Jwt jwt = new Jwt();
    private SuperAdmin superAdmin = new SuperAdmin();
    private Sso sso = new Sso();
    private Mfa mfa = new Mfa();
    private RateLimit rateLimit = new RateLimit();
    private SsoExchange ssoExchange = new SsoExchange();
    private Password password = new Password();
    /**
     * {@code ALL} or {@code OWNER_ONLY}. Owner-only applies only when the tenant has SSO enabled.
     */
    private String localLoginScope = "ALL";

    public boolean ownerOnlyLocalLogin() {
        return localLoginScope != null && localLoginScope.trim().equalsIgnoreCase("OWNER_ONLY");
    }
    private Cors cors = new Cors();
    private Session session = new Session();
    /** Throwaway demo endpoints under /api/demo/**: off in azure profile. */
    private boolean demoEndpointsEnabled = true;

    @Data
    public static class Jwt {
        /** HMAC signing secret (>= 32 bytes for HS256). Supplied via Key Vault in production. */
        private String secret;
        /** Access-token lifetime in minutes. */
        private long expiryMinutes = 480;
    }

    /** Bootstrap credentials for the first Super Admin (Arbor platform team). */
    @Data
    public static class SuperAdmin {
        private String email;
        private String password;
        private String fullName = "Arbor Platform Admin";
        /**
         * Emergency reset: a new value resets this Super Admin's password to {@link #password} at
         * startup and forces a change at next sign-in. Each value applies once. Blank = off.
         */
        private String resetCode = "";
    }

    /** SSO handshake settings. */
    @Data
    public static class Sso {
        /** Where to send the browser (with the issued JWT) after a successful SSO login. */
        private String frontendBaseUrl = "http://localhost:5173";
    }

    /** Local TOTP MFA settings (Path C B4). */
    @Data
    public static class Mfa {
        /**
         * Base64-encoded 32-byte AES key used to encrypt TOTP secrets at rest.
         * Falls back to a key derived from {@code jwt.secret} when unset (dev only) -
         * production deployments should supply a dedicated Key Vault-backed value.
         */
        private String encryptionKey;
        /** Lifetime, in minutes, of the short-lived MFA challenge token issued after password/OIDC check. */
        private long challengeExpiryMinutes = 5;
        /** Issuer name shown in authenticator apps. */
        private String issuer = "Arbor GRC";
    }

    /** Per-IP rate limits: in-memory single node; use API gateway / Redis at scale. */
    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int authMaxPerMinute = 20;
        private int otpRedeemMaxPerMinute = 10;
        private int tenantSsoDiscoverMaxPerMinute = 60;
        private int otpMaxFailures = 5;
        private int otpLockoutMinutes = 15;
    }

    @Data
    public static class Password {
        /** When true, require upper + lower + digit + special character (§4). Dev/E2E default false. */
        private boolean strict = false;
        private int minLength = 8;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
        /** Patterns such as https://*.arborgrc.com. Empty on local. */
        private List<String> allowedOriginPatterns = List.of();
        /**
         * When true, localhost origins are ignored. Set on the azure profile so a merged
         * local allowlist cannot stay open on UAT or prod.
         */
        private boolean dropLocalDevOrigins = false;

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = CorsOrigins.split(allowedOrigins);
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = CorsOrigins.split(allowedOriginPatterns);
        }

        public List<String> effectiveOrigins() {
            return CorsOrigins.effective(allowedOrigins, dropLocalDevOrigins);
        }

        public List<String> effectiveOriginPatterns() {
            return CorsOrigins.effective(allowedOriginPatterns, dropLocalDevOrigins);
        }
    }

    @Data
    public static class Session {
        /** Reject JWT when user disabled or token_version bumped. */
        private boolean validateTokenVersion = true;
    }

    /** One-time SSO code exchange (avoids JWT in URL fragment). */
    @Data
    public static class SsoExchange {
        private long codeTtlSeconds = 60;
    }
}
