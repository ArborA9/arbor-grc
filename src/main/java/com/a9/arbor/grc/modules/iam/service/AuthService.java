package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditAuthMethod;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.security.EntraSsoGuard;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import com.a9.arbor.grc.tenancy.TenantIds;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication against a tenant's user store. Because login happens before a JWT exists, the
 * target tenant is supplied explicitly (header) and set on the {@link TenantContext} for the
 * duration of the credential lookup.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final AuditTrailService auditTrailService;
    private final TenantRepository tenantRepository;
    private final MfaService mfaService;
    private final RefreshTokenService refreshTokenService;
    private final ArborSecurityProperties securityProperties;
    private final EntraSsoGuard entraSsoGuard;

    public AuthService(UserService userService, JwtService jwtService,
                       TenantDataSourceManager tenantDataSourceManager,
                       AuditTrailService auditTrailService,
                       TenantRepository tenantRepository,
                       MfaService mfaService,
                       RefreshTokenService refreshTokenService,
                       ArborSecurityProperties securityProperties,
                       EntraSsoGuard entraSsoGuard) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.auditTrailService = auditTrailService;
        this.tenantRepository = tenantRepository;
        this.mfaService = mfaService;
        this.refreshTokenService = refreshTokenService;
        this.securityProperties = securityProperties;
        this.entraSsoGuard = entraSsoGuard;
    }

    /** Verifies credentials against the given tenant and returns a signed JWT on success. */
    public AuthResult login(String tenantId, String email, String rawPassword) {
        String id = TenantIds.normalize(tenantId);
        tenantDataSourceManager.ensureRegistered(id);
        TenantContext.setTenantId(id);
        try {
            User user = userService.findByEmail(email)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .filter(User::isLocalLoginEnabled)
                    .filter(u -> u.getPasswordHash() != null)
                    .filter(u -> userService.matchesPassword(rawPassword, u.getPasswordHash()))
                    .filter(u -> localPasswordAllowed(id, u))
                    .orElseThrow(InvalidCredentialsException::new);

            if (MfaService.isMfaEligible(user) && mfaService.isEnabledFor(user)) {
                String mfaToken = jwtService.issueMfaChallenge(user.getEmail(), id);
                return AuthResult.mfaRequired(user, id, mfaToken);
            }

            return issueFullSession(id, user, false);
        } finally {
            TenantContext.clear();
        }
    }

    /** Signed-in LOCAL user changes their own password. Issues a fresh session. */
    public AuthResult changePassword(String email, String currentPassword, String newPassword) {
        User user = userService.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isLocalLoginEnabled() || user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password change applies to local accounts only");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }
        if (!userService.matchesPassword(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (currentPassword.equals(newPassword) || userService.matchesPassword(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must be different from the current password");
        }
        userService.setLocalPassword(user, newPassword);
        User fresh = userService.requireUser(user.getId());
        String tenantId = TenantContext.getTenantId();
        return issueTokens(tenantId, fresh, "LOCAL");
    }

    /** Completes local login after TOTP verification (Path C B4). */
    public AuthResult verifyMfa(String tenantIdHeader, String mfaToken, String code) {
        Claims claims;
        try {
            claims = jwtService.parse(mfaToken);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired MFA token");
        }
        if (!JwtService.isMfaChallenge(claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not an MFA challenge token");
        }
        String tenantId = claims.get(JwtService.CLAIM_TENANT, String.class);
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA token");
        }
        if (tenantIdHeader != null && !tenantIdHeader.isBlank()
                && !TenantIds.normalize(tenantIdHeader).equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant mismatch");
        }

        tenantDataSourceManager.ensureRegistered(tenantId);
        TenantContext.setTenantId(tenantId);
        try {
            User user = userService.findByEmail(claims.getSubject())
                    .filter(User::isLocalLoginEnabled)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .filter(u -> localPasswordAllowed(tenantId, u))
                    .orElseThrow(InvalidCredentialsException::new);
            if (!mfaService.verifyEnabledCode(user, code)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticator code");
            }
            return issueFullSession(tenantId, user, true);
        } finally {
            TenantContext.clear();
        }
    }

    private AuthResult issueFullSession(String tenantId, User user, boolean mfaVerified) {
        AuthResult issued = issueTokens(tenantId, user, "LOCAL");
        String roleSnap = user.getRoles().stream().map(Role::name).sorted().collect(Collectors.joining(","));
        // Password sign-in is audit-only, including on a workspace with Microsoft sign-in.
        auditTrailService.recordLogin(user.getId(), roleSnap, user.getEmail(), AuditAuthMethod.LOCAL, mfaVerified);
        return issued;
    }

    private AuthResult issueTokens(String tenantId, User user, String authMethod) {
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        String token = jwtService.issueForUser(user, tenantId, roles, authMethod);
        String refreshToken = refreshTokenService.issueForUser(user, authMethod);
        return AuthResult.full(token, refreshToken, user, tenantId, jwtService.getExpiryMinutes());
    }

    /** OWNER_ONLY plus tenant SSO: password sign-in is limited to Owners. Same failure as a bad password. */
    private boolean localPasswordAllowed(String tenantId, User user) {
        if (!localLoginRestricted(tenantId)) {
            return true;
        }
        return user.getRoles().contains(Role.OWNER);
    }

    private boolean localLoginRestricted(String tenantId) {
        if (!securityProperties.ownerOnlyLocalLogin() || tenantId == null) {
            return false;
        }
        return tenantRepository.findByTenantId(tenantId).map(t -> t.isSsoEnabled()).orElse(false);
    }

    /** Rotates a refresh token and issues a new JWT (and refresh token). */
    public AuthResult refresh(String tenantIdHeader, String rawRefreshToken) {
        String id = TenantIds.normalize(tenantIdHeader);
        tenantDataSourceManager.ensureRegistered(id);
        TenantContext.setTenantId(id);
        try {
            RefreshTokenService.RotatedSession rotated = refreshTokenService.rotate(rawRefreshToken);
            String method = rotated.authMethod() == null ? "LOCAL" : rotated.authMethod();
            if ("SSO".equals(method) && !entraSsoGuard.allow(id, rotated.user().getEmail())) {
                refreshTokenService.revokeAllForUser(rotated.user().getId());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session no longer valid");
            }
            return issueTokens(id, rotated.user(), method);
        } finally {
            TenantContext.clear();
        }
    }

    public void logout(String tenantIdHeader, String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        boolean opened = false;
        if (!TenantContext.hasTenant()) {
            String id = TenantIds.normalize(tenantIdHeader);
            if (id != null && !id.isBlank()) {
                tenantDataSourceManager.ensureRegistered(id);
                TenantContext.setTenantId(id);
                opened = true;
            }
        }
        if (!TenantContext.hasTenant()) {
            return;
        }
        try {
            refreshTokenService.revoke(rawRefreshToken);
        } finally {
            if (opened) {
                TenantContext.clear();
            }
        }
    }

    public record AuthResult(String token, String refreshToken, User user, String tenantId, long expiryMinutes,
                             boolean mfaRequired, String mfaToken) {
        static AuthResult full(String token, String refreshToken, User user, String tenantId, long expiryMinutes) {
            return new AuthResult(token, refreshToken, user, tenantId, expiryMinutes, false, null);
        }

        static AuthResult mfaRequired(User user, String tenantId, String mfaToken) {
            return new AuthResult(null, null, user, tenantId, 0, true, mfaToken);
        }
    }
}
