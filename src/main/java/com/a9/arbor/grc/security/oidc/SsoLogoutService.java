package com.a9.arbor.grc.security.oidc;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.directory.EntraIssuer;
import com.a9.arbor.grc.modules.iam.web.dto.LogoutResponse;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.SessionCookies;
import com.a9.arbor.grc.tenancy.TenantIds;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Builds the Entra sign-out redirect for an Arbor SSO session. Local and Super Admin
 * sign-out stay inside Arbor.
 */
@Service
public class SsoLogoutService {

    private final JwtService jwtService;
    private final TenantRepository tenantRepository;
    private final String signInUrl;

    public SsoLogoutService(JwtService jwtService,
                            TenantRepository tenantRepository,
                            ArborSecurityProperties properties) {
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
        String base = properties.getSso().getFrontendBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.signInUrl = base + "/signin";
    }

    public LogoutResponse forRequest(HttpServletRequest request, String tenantHeader, boolean endEntra) {
        if (!endEntra || !ssoAccess(request)) {
            return new LogoutResponse(null, false);
        }
        String tenantId = tenantId(request, tenantHeader);
        if (tenantId == null) {
            return new LogoutResponse(null, true);
        }
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !tenant.isSsoEnabled()) {
            return new LogoutResponse(null, true);
        }
        String url = EntraIssuer.endSessionUrl(tenant.getOidcIssuer(), signInUrl).orElse(null);
        return new LogoutResponse(url, true);
    }

    private boolean ssoAccess(HttpServletRequest request) {
        Claims claims = claims(request);
        return claims != null && "SSO".equals(claims.get(JwtService.CLAIM_AUTH_METHOD, String.class));
    }

    private String tenantId(HttpServletRequest request, String tenantHeader) {
        Claims claims = claims(request);
        String fromToken = claims == null ? null : claims.get(JwtService.CLAIM_TENANT, String.class);
        String id = fromToken == null || fromToken.isBlank() ? tenantHeader : fromToken;
        id = TenantIds.normalize(id);
        return id == null || id.isBlank() ? null : id;
    }

    private Claims claims(HttpServletRequest request) {
        String token = SessionCookies.accessToken(request);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtService.parse(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
