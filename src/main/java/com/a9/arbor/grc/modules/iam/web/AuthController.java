package com.a9.arbor.grc.modules.iam.web;

import com.a9.arbor.grc.catalog.web.SuperAdminAuthController;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.iam.service.AuthService;
import com.a9.arbor.grc.modules.iam.service.MfaService;
import com.a9.arbor.grc.modules.iam.service.PasswordResetService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.iam.web.dto.ChangePasswordRequest;
import com.a9.arbor.grc.modules.iam.web.dto.ForgotPasswordRequest;
import com.a9.arbor.grc.modules.iam.web.dto.ForgotPasswordResponse;
import com.a9.arbor.grc.modules.iam.web.dto.LoginRequest;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.modules.iam.web.dto.LogoutResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaCodeRequest;
import com.a9.arbor.grc.modules.iam.web.dto.MfaEnrollStartResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaStatusResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaVerifyRequest;
import com.a9.arbor.grc.modules.iam.web.dto.RefreshTokenRequest;
import com.a9.arbor.grc.modules.iam.web.dto.PasswordLinkStatusRequest;
import com.a9.arbor.grc.modules.iam.web.dto.PasswordLinkStatusResponse;
import com.a9.arbor.grc.modules.iam.web.dto.ResetPasswordRequest;
import com.a9.arbor.grc.modules.iam.web.dto.ResetPasswordResponse;
import com.a9.arbor.grc.modules.iam.web.dto.SessionView;
import com.a9.arbor.grc.modules.iam.web.dto.UserResponse;
import com.a9.arbor.grc.security.JwtAuthenticationFilter;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.SessionCookies;
import com.a9.arbor.grc.security.oidc.SsoLogoutService;
import io.jsonwebtoken.Claims;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final AuthService authService;
    private final UserService userService;
    private final MfaService mfaService;
    private final PasswordResetService passwordResetService;
    private final SessionCookies sessionCookies;
    private final SsoLogoutService ssoLogoutService;
    private final AdminModuleAccessService adminModuleAccessService;

    public AuthController(AuthService authService, UserService userService, MfaService mfaService,
                          PasswordResetService passwordResetService, SessionCookies sessionCookies,
                          SsoLogoutService ssoLogoutService,
                          AdminModuleAccessService adminModuleAccessService) {
        this.authService = authService;
        this.userService = userService;
        this.mfaService = mfaService;
        this.passwordResetService = passwordResetService;
        this.sessionCookies = sessionCookies;
        this.ssoLogoutService = ssoLogoutService;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    /** Login requires the tenant (header) since no token exists yet; returns a JWT on success. */
    @PostMapping("/login")
    public LoginResponse login(@RequestHeader(TENANT_HEADER) String tenantId,
                               @Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.login(tenantId, request.email(), request.password());
        if (result.mfaRequired()) {
            return LoginResponse.mfaChallenge(result.user().getEmail(), result.tenantId(), result.mfaToken());
        }
        List<String> roles = result.user().getRoles().stream().map(Role::name).sorted().toList();
        LoginResponse body = LoginResponse.full(result.token(), result.user().getEmail(), result.tenantId(), roles,
                result.expiryMinutes(), result.refreshToken(), result.user().getAuthType().name());
        return finishBrowserSession(httpRequest, httpResponse, body, Boolean.TRUE.equals(request.rememberMe()));
    }

    /** Path C B4: complete local login with TOTP after password challenge. */
    @PostMapping("/mfa/verify")
    public LoginResponse verifyMfa(@RequestHeader(value = TENANT_HEADER, required = false) String tenantId,
                                   @Valid @RequestBody MfaVerifyRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.verifyMfa(tenantId, request.mfaToken(), request.code());
        List<String> roles = result.user().getRoles().stream().map(Role::name).sorted().toList();
        LoginResponse body = LoginResponse.full(result.token(), result.user().getEmail(), result.tenantId(), roles,
                result.expiryMinutes(), result.refreshToken(), result.user().getAuthType().name());
        return finishBrowserSession(httpRequest, httpResponse, body, SessionCookies.persistent(httpRequest)
                || Boolean.TRUE.equals(request.rememberMe()));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestHeader(TENANT_HEADER) String tenantId,
                                 @RequestBody(required = false) RefreshTokenRequest request,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        try {
            String raw = refreshToken(request, httpRequest);
            AuthService.AuthResult result = authService.refresh(tenantId, raw);
            List<String> roles = result.user().getRoles().stream().map(Role::name).sorted().toList();
            LoginResponse body = LoginResponse.full(result.token(), result.user().getEmail(), result.tenantId(), roles,
                    result.expiryMinutes(), result.refreshToken(), result.user().getAuthType().name());
            return finishBrowserSession(httpRequest, httpResponse, body, SessionCookies.persistent(httpRequest));
        } catch (ResponseStatusException ex) {
            if (SessionCookies.requested(httpRequest)) {
                sessionCookies.clear(httpRequest, httpResponse);
            }
            throw ex;
        }
    }

    @PostMapping("/logout")
    public LogoutResponse logout(@RequestHeader(value = TENANT_HEADER, required = false) String tenantId,
                                 @RequestParam(value = "endEntra", defaultValue = "false") boolean endEntra,
                                 @RequestBody(required = false) RefreshTokenRequest request,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        LogoutResponse entra = ssoLogoutService.forRequest(httpRequest, tenantId, endEntra);
        String raw = null;
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            raw = request.refreshToken();
        } else {
            raw = SessionCookies.refreshToken(httpRequest);
        }
        authService.logout(tenantId, raw);
        sessionCookies.clear(httpRequest, httpResponse);
        if (httpRequest.getSession(false) != null) {
            try {
                httpRequest.getSession(false).invalidate();
            } catch (IllegalStateException ignored) {
                // Already invalidated for this request.
            }
        }
        return entra;
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestHeader(TENANT_HEADER) String tenantId,
                                                 @Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(tenantId, request.email());
    }

    @PostMapping("/password-link")
    public PasswordLinkStatusResponse passwordLink(@RequestHeader(TENANT_HEADER) String tenantId,
                                                   @RequestBody PasswordLinkStatusRequest request) {
        return passwordResetService.linkStatus(tenantId, request.token());
    }

    @PostMapping("/reset-password")
    public ResetPasswordResponse resetPassword(@RequestHeader(TENANT_HEADER) String tenantId,
                                               @Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.completeReset(tenantId, request.token(), request.newPassword());
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public LoginResponse changePassword(Authentication authentication,
                                        @Valid @RequestBody ChangePasswordRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use the Super Admin password change");
        }
        AuthService.AuthResult result = authService.changePassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
        List<String> roles = result.user().getRoles().stream().map(Role::name).sorted().toList();
        LoginResponse body = LoginResponse.full(result.token(), result.user().getEmail(), result.tenantId(), roles,
                result.expiryMinutes(), result.refreshToken(), result.user().getAuthType().name());
        return finishBrowserSession(httpRequest, httpResponse, body, SessionCookies.persistent(httpRequest));
    }

    @GetMapping("/mfa/status")
    @PreAuthorize("isAuthenticated()")
    public MfaStatusResponse mfaStatus(Authentication authentication) {
        return mfaService.status(requireUser(authentication));
    }

    @PostMapping("/mfa/enroll/start")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public MfaEnrollStartResponse enrollStart(Authentication authentication) {
        return mfaService.beginEnroll(requireUser(authentication));
    }

    @PostMapping("/mfa/enroll/confirm")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public MfaStatusResponse enrollConfirm(Authentication authentication,
                                           @Valid @RequestBody MfaCodeRequest request) {
        return mfaService.confirmEnroll(requireUser(authentication), request.code());
    }

    @DeleteMapping("/mfa")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public MfaStatusResponse disableMfa(Authentication authentication,
                                        @Valid @RequestBody MfaCodeRequest request) {
        return mfaService.disable(requireUser(authentication), request.code());
    }

    /** Who the cookie or bearer token is. Does not return the token. */
    @GetMapping("/session")
    public SessionView session(HttpServletRequest request) {
        Object raw = request.getAttribute(JwtAuthenticationFilter.CLAIMS_ATTR);
        if (!(raw instanceof Claims claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session no longer valid");
        }
        List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
        if (roles == null) {
            roles = List.of();
        }
        boolean superAdmin = roles.stream().anyMatch(r -> "SUPER_ADMIN".equals(r)
                || SuperAdminAuthController.PASSWORD_CHANGE_ROLE.equals(r));
        return new SessionView(
                claims.getSubject(),
                claims.get(JwtService.CLAIM_TENANT, String.class),
                roles,
                superAdmin,
                claims.get(JwtService.CLAIM_AUTH_METHOD, String.class));
    }

    /** Returns the authenticated user's profile (tenant comes from the validated token). */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElseThrow();
        return UserResponse.from(user, userService.orgUnitIdsFor(user.getId()),
                adminModuleAccessService.codesFor(user));
    }

    private LoginResponse finishBrowserSession(HttpServletRequest request, HttpServletResponse response,
                                               LoginResponse body, boolean persistent) {
        if (!SessionCookies.requested(request) || body.mfaRequired()) {
            return body;
        }
        sessionCookies.write(request, response, body.token(), body.refreshToken(), body.expiresInMinutes(), persistent);
        return body.withoutSecrets();
    }

    private static String refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        String cookie = SessionCookies.refreshToken(httpRequest);
        if (cookie == null || cookie.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return cookie;
    }

    private User requireUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
    }
}
