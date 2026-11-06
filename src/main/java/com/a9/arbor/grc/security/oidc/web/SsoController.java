package com.a9.arbor.grc.security.oidc.web;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.security.oidc.SsoPromptAuthorizationRequestResolver;
import com.a9.arbor.grc.security.oidc.TenantClientRegistrationRepository;
import com.a9.arbor.grc.tenancy.TenantIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

/**
 * SSO entry + discovery.
 *
 * <ul>
 *   <li>{@code GET /sso/{tenantId}}: tenant-aware entry: if SSO is enabled and the IdP config
 *       resolves, redirect into {@code /oauth2/authorization/{tenantId}} for a seamless login.</li>
 *   <li>{@code GET /api/auth/tenant/{tenantId}/sso}: public status for the SPA SSO button.</li>
 * </ul>
 */
@Controller
public class SsoController {

    private final TenantRepository tenantRepository;
    private final TenantClientRegistrationRepository clientRegistrationRepository;
    private final String frontendBaseUrl;

    public SsoController(TenantRepository tenantRepository,
                         TenantClientRegistrationRepository clientRegistrationRepository,
                         ArborSecurityProperties properties) {
        this.tenantRepository = tenantRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.frontendBaseUrl = properties.getSso().getFrontendBaseUrl();
    }

    @GetMapping("/sso/{tenantId}")
    public RedirectView start(@PathVariable String tenantId,
                              @RequestParam(value = "reauth", required = false) String reauth,
                              HttpServletRequest request) {
        String id = TenantIds.normalize(tenantId);
        Tenant tenant = tenantRepository.findByTenantId(id).orElse(null);
        if (tenant == null || !tenant.isSsoEnabled()) {
            return new RedirectView(frontendBaseUrl + "/signin#sso_error=sso_not_enabled");
        }
        // Resolve (and cache) the OIDC registration up front so a bad issuer/secret becomes a
        // clear SPA error instead of Spring's default /login bounce.
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(id);
        if (registration == null) {
            return new RedirectView(frontendBaseUrl + "/signin#sso_error=idp_misconfigured");
        }
        if ("1".equals(reauth)) {
            request.getSession(true).setAttribute(SsoPromptAuthorizationRequestResolver.SESSION_PROMPT, "login");
        }
        return new RedirectView("/oauth2/authorization/" + id);
    }

    @GetMapping("/api/auth/tenant/{tenantId}/sso")
    @ResponseBody
    public TenantSsoStatus status(@PathVariable String tenantId) {
        String id = TenantIds.normalize(tenantId);
        return tenantRepository.findByTenantId(id)
                .map(t -> new TenantSsoStatus(t.getTenantId(), t.getName(), t.isSsoEnabled()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    }

    /** Public, non-sensitive SSO status for a tenant (never exposes client id/secret). */
    public record TenantSsoStatus(String tenantId, String name, boolean ssoEnabled) {
    }
}
