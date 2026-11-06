package com.a9.arbor.grc.security;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Issues and validates HS256 JWTs carrying the tenant id and the user's roles. */
@Service
public class JwtService {

    public static final String CLAIM_TENANT = "tenant_id";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PURPOSE = "purpose";
    public static final String CLAIM_TOKEN_VERSION = "tv";
    /** LOCAL or SSO. Absent on tokens issued before this claim existed. */
    public static final String CLAIM_AUTH_METHOD = "am";
    /** Invite row id for a TPRM vendor-portal session. */
    public static final String CLAIM_VENDOR_TOKEN_ID = "vtid";
    public static final String PURPOSE_MFA_CHALLENGE = "mfa_challenge";
    public static final String PURPOSE_VENDOR_PORTAL = "tprm_vendor";

    private final SecretKey key;
    private final long expiryMinutes;
    private final long mfaChallengeExpiryMinutes;

    public JwtService(ArborSecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = properties.getJwt().getExpiryMinutes();
        this.mfaChallengeExpiryMinutes = properties.getMfa().getChallengeExpiryMinutes();
    }

    /** Issues a token. A null {@code tenantId} yields a tenant-less token (used for Super Admins). */
    public String issue(String email, String tenantId, List<String> roles) {
        return issue(email, tenantId, roles, expiryMinutes, null);
    }

    /** Issues a token with a custom lifetime (minutes). Used for scoped external-audit OTP sessions. */
    public String issue(String email, String tenantId, List<String> roles, long expiryMinutesOverride) {
        return issue(email, tenantId, roles, expiryMinutesOverride, null);
    }

    /** Tenant session token including token-version claim for revocation. */
    public String issueForUser(User user, String tenantId, List<String> roles) {
        return issue(user.getEmail(), tenantId, roles, expiryMinutes, user.getTokenVersion());
    }

    public String issueForUser(User user, String tenantId, List<String> roles, String authMethod) {
        return issue(user.getEmail(), tenantId, roles, expiryMinutes, user.getTokenVersion(), null, authMethod);
    }

    public String issueForUser(User user, String tenantId, List<String> roles, long expiryMinutesOverride) {
        return issue(user.getEmail(), tenantId, roles, expiryMinutesOverride, user.getTokenVersion(), null);
    }

    /** Vendor portal JWT bound to one invite / questionnaire, not "latest ACTIVE token for this email". */
    public String issueForVendorSession(User user, String tenantId, List<String> roles,
                                        long expiryMinutesOverride, long vendorTokenId) {
        return issue(user.getEmail(), tenantId, roles, expiryMinutesOverride, user.getTokenVersion(), vendorTokenId);
    }

    public static Long vendorTokenId(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object raw = claims.get(CLAIM_VENDOR_TOKEN_ID);
        if (raw instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private String issue(String email, String tenantId, List<String> roles,
                         long expiryMinutesOverride, Integer tokenVersion) {
        return issue(email, tenantId, roles, expiryMinutesOverride, tokenVersion, null, null);
    }

    private String issue(String email, String tenantId, List<String> roles,
                         long expiryMinutesOverride, Integer tokenVersion, Long vendorTokenId) {
        return issue(email, tenantId, roles, expiryMinutesOverride, tokenVersion, vendorTokenId, null);
    }

    private String issue(String email, String tenantId, List<String> roles,
                         long expiryMinutesOverride, Integer tokenVersion, Long vendorTokenId,
                         String authMethod) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(email)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiryMinutesOverride * 60)));
        if (tenantId != null) {
            builder.claim(CLAIM_TENANT, tenantId);
            builder.claim(CLAIM_TOKEN_VERSION, tokenVersion == null ? 0 : tokenVersion);
        }
        if (vendorTokenId != null) {
            builder.claim(CLAIM_VENDOR_TOKEN_ID, vendorTokenId);
            builder.claim(CLAIM_PURPOSE, PURPOSE_VENDOR_PORTAL);
        }
        if (authMethod != null && !authMethod.isBlank()) {
            builder.claim(CLAIM_AUTH_METHOD, authMethod);
        }
        return builder.signWith(key).compact();
    }

    /** Short-lived token after password check: only valid for {@code POST /api/auth/mfa/verify}. */
    public String issueMfaChallenge(String email, String tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_TENANT, tenantId)
                .claim(CLAIM_PURPOSE, PURPOSE_MFA_CHALLENGE)
                .claim(CLAIM_ROLES, List.of())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(mfaChallengeExpiryMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public static boolean isMfaChallenge(Claims claims) {
        return PURPOSE_MFA_CHALLENGE.equals(claims.get(CLAIM_PURPOSE, String.class));
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiryMinutes() {
        return expiryMinutes;
    }
}
