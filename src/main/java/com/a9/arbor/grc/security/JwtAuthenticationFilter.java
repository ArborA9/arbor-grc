package com.a9.arbor.grc.security;

import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import com.a9.arbor.grc.tenancy.UnknownTenantException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the Bearer JWT on each request and, if valid, establishes both the Spring Security
 * authentication (with {@code ROLE_*} authorities) and the {@link TenantContext} (from the token's
 * tenant claim). This is what replaces header-based tenant resolution for authenticated traffic.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CLAIMS_ATTR = "arbor.jwt.claims";

    private final JwtService jwtService;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final JwtSessionValidator jwtSessionValidator;
    private final EntraSsoGuard entraSsoGuard;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TenantDataSourceManager tenantDataSourceManager,
                                   JwtSessionValidator jwtSessionValidator,
                                   EntraSsoGuard entraSsoGuard) {
        this.jwtService = jwtService;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.jwtSessionValidator = jwtSessionValidator;
        this.entraSsoGuard = entraSsoGuard;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parse(token);
            if (JwtService.isMfaChallenge(claims)) {
                // Challenge tokens are only for /api/auth/mfa/verify: do not authenticate.
                filterChain.doFilter(request, response);
                return;
            }
            String tenantId = claims.get(JwtService.CLAIM_TENANT, String.class);
            List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
            if (roles == null) {
                roles = List.of();
            }

            var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(CLAIMS_ATTR, claims);

            // Tenant-less tokens (Super Admins) operate on the catalog only: no tenant routing.
            if (tenantId != null) {
                tenantDataSourceManager.ensureRegistered(tenantId);
                TenantContext.setTenantId(tenantId);
                if (!jwtSessionValidator.isValidTenantSession(tenantId, claims)) {
                    rejectOrContinue(request, response, filterChain, "Session no longer valid");
                    return;
                }
                if (isSsoStaffSession(claims) && !entraSsoGuard.allow(tenantId, claims.getSubject())) {
                    rejectOrContinue(request, response, filterChain, "Session no longer valid");
                    return;
                }
            }
            VendorPortalSession.setTokenId(JwtService.vendorTokenId(claims));
        } catch (JwtException | UnknownTenantException e) {
            rejectOrContinue(request, response, filterChain, "Invalid or expired token");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            VendorPortalSession.clear();
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** Bearer wins so scripts and the vendor portal stay on Authorization. The SPA sends the cookie. */
    private static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return SessionCookies.accessToken(request);
    }

    private static boolean isSsoStaffSession(Claims claims) {
        if (JwtService.vendorTokenId(claims) != null) {
            return false;
        }
        return "SSO".equals(claims.get(JwtService.CLAIM_AUTH_METHOD, String.class));
    }

    /**
     * Login, refresh, and logout must still run when the access cookie is expired or Entra
     * has disabled the user, so the browser can replace or clear that cookie.
     */
    private void rejectOrContinue(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain, String message) throws IOException, ServletException {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        request.removeAttribute(CLAIMS_ATTR);
        boolean pageLoad = isPageOrAssetGet(request);
        if (pageLoad) {
            SessionCookies.clearAccess(request, response);
        }
        if (pageLoad || isSessionMaintenance(request)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                VendorPortalSession.clear();
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            }
            return;
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    /**
     * A stale access cookie (deleted tenant or user, expired token) must not block the sign-in page or
     * its JS/CSS, or the browser can never reach sign-in. Those load anonymously; /api stays 401.
     */
    private static boolean isPageOrAssetGet(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        String path = request.getRequestURI();
        return path != null && !path.startsWith("/api/") && !path.startsWith("/actuator");
    }

    private static boolean isSessionMaintenance(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/login".equals(path)
                || "/api/auth/mfa/verify".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/auth/logout".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path)
                || "/api/auth/password-link".equals(path)
                || "/api/auth/sso/exchange".equals(path)
                || "/api/auth/super-admin/login".equals(path)
                || (path != null && path.startsWith("/api/auth/tenant/"));
    }
}
