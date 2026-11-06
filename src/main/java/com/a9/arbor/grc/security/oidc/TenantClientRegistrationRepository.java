package com.a9.arbor.grc.security.oidc;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves an OIDC {@link ClientRegistration} <b>per tenant</b> from the catalog IdP config, instead
 * of the usual static application.yml registrations. The Spring OAuth2 login endpoints use the
 * {@code registrationId} in the URL (e.g. {@code /oauth2/authorization/acme}): here the
 * registrationId <b>is the tenantId</b>.
 *
 * <p>The registration is built by OIDC discovery against the tenant's issuer (e.g. Entra ID) and
 * cached until the tenant's IdP config changes (see {@link #evict}). Returns {@code null} when the
 * tenant is unknown or SSO is not enabled: Spring then rejects the login attempt.
 */
@Component
public class TenantClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger log = LoggerFactory.getLogger(TenantClientRegistrationRepository.class);

    private final TenantRepository tenantRepository;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TenantClientRegistrationRepository(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Tenant tenant = tenantRepository.findByTenantId(registrationId).orElse(null);
        if (tenant == null || !tenant.isSsoEnabled()
                || isBlank(tenant.getOidcIssuer()) || isBlank(tenant.getOidcClientId())) {
            return null;
        }

        String key = tenant.getOidcIssuer() + "|" + tenant.getOidcClientId() + "|" + tenant.getOidcClientSecret();
        CacheEntry entry = cache.get(registrationId);
        if (entry != null && entry.key().equals(key)) {
            return entry.registration();
        }

        try {
            // OIDC discovery: reads <issuer>/.well-known/openid-configuration (network call to the IdP).
            ClientRegistration registration = ClientRegistrations.fromIssuerLocation(tenant.getOidcIssuer())
                    .registrationId(registrationId)
                    .clientId(tenant.getOidcClientId())
                    .clientSecret(tenant.getOidcClientSecret())
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .clientName("Arbor tenant " + registrationId)
                    .build();
            cache.put(registrationId, new CacheEntry(key, registration));
            return registration;
        } catch (RuntimeException e) {
            log.warn("Failed to build OIDC client registration for tenant {} (issuer {}): {}",
                    registrationId, tenant.getOidcIssuer(), e.getMessage());
            return null;
        }
    }

    /** Drop the cached registration for a tenant (call after its IdP config changes). */
    public void evict(String tenantId) {
        cache.remove(tenantId);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record CacheEntry(String key, ClientRegistration registration) {
    }
}
