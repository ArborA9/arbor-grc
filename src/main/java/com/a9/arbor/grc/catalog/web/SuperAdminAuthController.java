package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.domain.SuperAdmin;
import com.a9.arbor.grc.catalog.service.SuperAdminMfaService;
import com.a9.arbor.grc.catalog.service.SuperAdminService;
import com.a9.arbor.grc.modules.iam.web.dto.ChangePasswordRequest;
import com.a9.arbor.grc.modules.iam.web.dto.LoginRequest;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaCodeRequest;
import com.a9.arbor.grc.modules.iam.web.dto.MfaEnrollStartResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaStatusResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaVerifyRequest;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.SessionCookies;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Super Admin (Arbor platform team) authentication. Unlike tenant login, this requires no tenant -
 * the issued token carries the SUPER_ADMIN role and no tenant claim.
 */
@RestController
@RequestMapping("/api/auth/super-admin")
public class SuperAdminAuthController {

    private static final List<String> ROLES = List.of("SUPER_ADMIN");
    /** After an emergency reset: the token can only call change-password. */
    public static final String PASSWORD_CHANGE_ROLE = "SUPER_ADMIN_PASSWORD_CHANGE";
    private static final List<String> PASSWORD_CHANGE_ROLES = List.of(PASSWORD_CHANGE_ROLE);

    private final SuperAdminService superAdminService;
    private final SuperAdminMfaService mfaService;
    private final JwtService jwtService;
    private final SessionCookies sessionCookies;

    public SuperAdminAuthController(SuperAdminService superAdminService,
                                    SuperAdminMfaService mfaService,
                                    JwtService jwtService,
                                    SessionCookies sessionCookies) {
        this.superAdminService = superAdminService;
        this.mfaService = mfaService;
        this.jwtService = jwtService;
        this.sessionCookies = sessionCookies;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        SuperAdmin admin = superAdminService.authenticate(request.email(), request.password());
        if (admin.isMustChangePassword()) {
            String token = jwtService.issue(admin.getEmail(), null, PASSWORD_CHANGE_ROLES);
            return finish(httpRequest, httpResponse, token, admin.getEmail(), PASSWORD_CHANGE_ROLES);
        }
        if (mfaService.isEnabledFor(admin)) {
            String mfaToken = jwtService.issueSuperAdminMfaChallenge(admin.getEmail());
            return LoginResponse.mfaChallenge(admin.getEmail(), null, mfaToken);
        }
        String token = jwtService.issue(admin.getEmail(), null, ROLES);
        return finish(httpRequest, httpResponse, token, admin.getEmail(), ROLES);
    }

    @PostMapping("/mfa/verify")
    public LoginResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        Claims claims;
        try {
            claims = jwtService.parse(request.mfaToken());
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired MFA token");
        }
        if (!JwtService.isSuperAdminMfaChallenge(claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a Super Admin MFA challenge token");
        }
        SuperAdmin admin = superAdminService.requireActiveByEmail(claims.getSubject());
        if (admin.isMustChangePassword()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password change required");
        }
        if (!mfaService.verifyEnabledCode(admin, request.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticator code");
        }
        String token = jwtService.issue(admin.getEmail(), null, ROLES);
        return finish(httpRequest, httpResponse, token, admin.getEmail(), ROLES);
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ADMIN_PASSWORD_CHANGE')")
    public LoginResponse changePassword(Authentication authentication,
                                        @Valid @RequestBody ChangePasswordRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        SuperAdmin admin = superAdminService.changePassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
        if (mfaService.isEnabledFor(admin)) {
            String mfaToken = jwtService.issueSuperAdminMfaChallenge(admin.getEmail());
            return LoginResponse.mfaChallenge(admin.getEmail(), null, mfaToken);
        }
        String token = jwtService.issue(admin.getEmail(), null, ROLES);
        return finish(httpRequest, httpResponse, token, admin.getEmail(), ROLES);
    }

    @GetMapping("/mfa/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MfaStatusResponse mfaStatus(Authentication authentication) {
        return mfaService.status(requireAdmin(authentication));
    }

    @PostMapping("/mfa/enroll/start")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MfaEnrollStartResponse enrollStart(Authentication authentication) {
        return mfaService.beginEnroll(requireAdmin(authentication));
    }

    @PostMapping("/mfa/enroll/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MfaStatusResponse enrollConfirm(Authentication authentication,
                                           @Valid @RequestBody MfaCodeRequest request) {
        return mfaService.confirmEnroll(requireAdmin(authentication), request.code());
    }

    @DeleteMapping("/mfa")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MfaStatusResponse disableMfa(Authentication authentication,
                                        @Valid @RequestBody MfaCodeRequest request) {
        return mfaService.disable(requireAdmin(authentication), request.code());
    }

    private SuperAdmin requireAdmin(Authentication authentication) {
        return superAdminService.requireActiveByEmail(authentication.getName());
    }

    private LoginResponse finish(HttpServletRequest request, HttpServletResponse response,
                                 String token, String email, List<String> roles) {
        LoginResponse body = LoginResponse.full(token, email, null, roles, jwtService.getExpiryMinutes(), null, "LOCAL");
        if (!SessionCookies.requested(request)) {
            return body;
        }
        sessionCookies.write(request, response, token, null, body.expiresInMinutes(), true);
        return body.withoutSecrets();
    }
}
