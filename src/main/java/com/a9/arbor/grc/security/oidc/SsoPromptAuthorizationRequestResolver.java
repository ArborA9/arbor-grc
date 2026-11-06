package com.a9.arbor.grc.security.oidc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds {@code prompt=login} to one Entra authorization request after Arbor sign-out.
 * A normal SSO click, while the Microsoft session is still valid, stays silent.
 */
@Component
public class SsoPromptAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String SESSION_PROMPT = "ARBOR_SSO_PROMPT";

    private final OAuth2AuthorizationRequestResolver delegate;

    public SsoPromptAuthorizationRequestResolver(TenantClientRegistrationRepository registrations) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                registrations, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return applyPrompt(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return applyPrompt(request, delegate.resolve(request, registrationId));
    }

    private static OAuth2AuthorizationRequest applyPrompt(HttpServletRequest request,
                                                          OAuth2AuthorizationRequest resolved) {
        if (resolved == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null || !"login".equals(session.getAttribute(SESSION_PROMPT))) {
            return resolved;
        }
        session.removeAttribute(SESSION_PROMPT);
        Map<String, Object> extra = new LinkedHashMap<>(resolved.getAdditionalParameters());
        extra.put("prompt", "login");
        return OAuth2AuthorizationRequest.from(resolved).additionalParameters(extra).build();
    }
}
