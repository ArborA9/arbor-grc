package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.domain.SuperAdmin;
import com.a9.arbor.grc.catalog.service.SuperAdminService;
import com.a9.arbor.grc.modules.iam.web.dto.ChangePasswordRequest;
import com.a9.arbor.grc.modules.iam.web.dto.LoginRequest;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.SessionCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final JwtService jwtService;
    private final SessionCookies sessionCookies;

    public SuperAdminAuthController(SuperAdminService superAdminService, JwtService jwtService,
                                    SessionCookies sessionCookies) {
        this.superAdminService = superAdminService;
        this.jwtService = jwtService;
        this.sessionCookies = sessionCookies;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        SuperAdmin admin = superAdminService.authenticate(request.email(), request.password());
        List<String> roles = admin.isMustChangePassword() ? PASSWORD_CHANGE_ROLES : ROLES;
        String token = jwtService.issue(admin.getEmail(), null, roles);
        return finish(httpRequest, httpResponse, token, admin.getEmail(), roles);
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_ADMIN_PASSWORD_CHANGE')")
    public LoginResponse changePassword(Authentication authentication,
                                        @Valid @RequestBody ChangePasswordRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        SuperAdmin admin = superAdminService.changePassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
        String token = jwtService.issue(admin.getEmail(), null, ROLES);
        return finish(httpRequest, httpResponse, token, admin.getEmail(), ROLES);
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
