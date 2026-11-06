package com.a9.arbor.grc.security.oidc;

import com.a9.arbor.grc.catalog.domain.JitDefaultRole;
import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditAuthMethod;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.SsoGroupRoleMappingService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.sso.SsoExchangeService;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Terminates the SSO handshake: resolve tenant from OAuth2 registrationId, match verified email to
 * an Arbor user (or JIT-provision when the tenant allows it), apply IdP group→role mappings, mint
 * the app JWT, redirect to the SPA. Authorization roles always live in Arbor.
 */
@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcLoginSuccessHandler.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final AuditTrailService auditTrailService;
    private final NotificationService notificationService;
    private final TenantRepository tenantRepository;
    private final SsoGroupRoleMappingService groupRoleMappingService;
    private final SsoExchangeService ssoExchangeService;
    private final String frontendBaseUrl;

    public OidcLoginSuccessHandler(UserService userService, JwtService jwtService,
                                   TenantDataSourceManager tenantDataSourceManager,
                                   AuditTrailService auditTrailService,
                                   NotificationService notificationService,
                                   TenantRepository tenantRepository,
                                   SsoGroupRoleMappingService groupRoleMappingService,
                                   SsoExchangeService ssoExchangeService,
                                   ArborSecurityProperties properties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.auditTrailService = auditTrailService;
        this.notificationService = notificationService;
        this.tenantRepository = tenantRepository;
        this.groupRoleMappingService = groupRoleMappingService;
        this.ssoExchangeService = ssoExchangeService;
        this.frontendBaseUrl = properties.getSso().getFrontendBaseUrl();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String tenantId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        if (isBlank(email)) {
            email = oidcUser.getPreferredUsername();
        }

        if (isBlank(email)) {
            log.warn("SSO login for tenant {} returned no email/preferred_username claim", tenantId);
            response.sendRedirect(redirectError("no_email", null));
            return;
        }
        email = email.trim().toLowerCase();

        List<String> idpGroups = readGroups(oidcUser);
        boolean mfaVerified = readMfaVerified(oidcUser);

        tenantDataSourceManager.ensureRegistered(tenantId);
        TenantContext.setTenantId(tenantId);
        try {
            Optional<User> matched = userService.completeSsoLogin(email);
            if (matched.isEmpty()) {
                matched = tryJitProvision(tenantId, email, idpGroups);
            }
            if (matched.isEmpty()) {
                log.info("SSO login denied for {} on tenant {}: no invited account", email, tenantId);
                notificationService.trigger(
                        "SSO_NO_ROLE",
                        email,
                        List.of("ADMIN"),
                        Map.of("email", email));
                response.sendRedirect(redirectError("no_account", email));
                return;
            }
            User user = matched.get();
            Set<Role> mappedRoles = groupRoleMappingService.resolveRoles(idpGroups);
            user = userService.unionRoles(user, mappedRoles);

            List<String> roles = user.getRoles().stream().map(Role::name).toList();
            String token = jwtService.issueForUser(user, tenantId, roles, "SSO");
            String roleSnap = roles.stream().sorted().collect(Collectors.joining(","));
            auditTrailService.recordLogin(user.getId(), roleSnap, user.getEmail(), AuditAuthMethod.SSO, mfaVerified);
            log.info("SSO login success for {} on tenant {} (mfaVerified={})", user.getEmail(), tenantId, mfaVerified);
            String code = ssoExchangeService.issueCode(
                    token, tenantId, user.getEmail(), roles, jwtService.getExpiryMinutes());
            response.sendRedirect(frontendBaseUrl + "/signin#sso_code=" + enc(code));
        } finally {
            TenantContext.clear();
        }
    }

    /** Path C B1: auto-creates the account when JIT is on and default role is CONTRIBUTOR. */
    private Optional<User> tryJitProvision(String tenantId, String email, List<String> idpGroups) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !tenant.isJitEnabled()) {
            return Optional.empty();
        }
        if (tenant.getJitDefaultRole() != JitDefaultRole.CONTRIBUTOR) {
            // BLOCK (or unknown): deny: caller fires SSO_NO_ROLE
            log.info("JIT enabled but default role is {} for {} on tenant {}: blocking",
                    tenant.getJitDefaultRole(), email, tenantId);
            return Optional.empty();
        }
        try {
            User created = userService.provisionJitUser(email, Role.CONTRIBUTOR);
            Set<Role> mappedRoles = groupRoleMappingService.resolveRoles(idpGroups);
            created = userService.unionRoles(created, mappedRoles);
            return Optional.of(created);
        } catch (RuntimeException e) {
            log.warn("JIT provisioning failed for {} on tenant {}: {}", email, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Best-effort read of the IdP "groups" claim (Entra emits object-ids here; overage tenants
     * instead emit a "_claim_names" pointer that we don't resolve: those users simply get no
     * group-mapped roles until an Admin assigns them directly).
     */
    @SuppressWarnings("unchecked")
    private static List<String> readGroups(OidcUser oidcUser) {
        Object claim = oidcUser.getClaims().get("groups");
        if (claim instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * Path C B4: trusts the IdP's own MFA enforcement via the standard {@code amr} (Authentication
     * Methods References) / {@code acr} (Authentication Context Class Reference) claims rather than
     * re-implementing MFA for SSO users.
     */
    @SuppressWarnings("unchecked")
    private static boolean readMfaVerified(OidcUser oidcUser) {
        Object amr = oidcUser.getClaims().get("amr");
        if (amr instanceof List<?> list) {
            for (Object v : list) {
                String s = String.valueOf(v).toLowerCase(Locale.ROOT);
                if (s.contains("mfa") || s.equals("otp") || s.contains("multifactor")) {
                    return true;
                }
            }
        } else if (amr instanceof String s && s.toLowerCase(Locale.ROOT).contains("mfa")) {
            return true;
        }
        Object acr = oidcUser.getClaims().get("acr");
        if (acr != null) {
            String s = String.valueOf(acr).toLowerCase(Locale.ROOT);
            if (s.contains("mfa") || s.contains("multifactor")) {
                return true;
            }
        }
        return false;
    }

    private String redirectError(String code, String email) {
        StringBuilder sb = new StringBuilder(frontendBaseUrl).append("/signin#sso_error=").append(enc(code));
        if (email != null) {
            sb.append("&email=").append(enc(email));
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
