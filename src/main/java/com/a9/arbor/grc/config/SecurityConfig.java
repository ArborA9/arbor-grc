package com.a9.arbor.grc.config;

import com.a9.arbor.grc.security.AuditorAccessFilter;
import com.a9.arbor.grc.security.JwtAuthenticationFilter;
import com.a9.arbor.grc.security.LicenseWriteLockFilter;
import com.a9.arbor.grc.security.oidc.OidcLoginFailureHandler;
import com.a9.arbor.grc.security.oidc.OidcLoginSuccessHandler;
import com.a9.arbor.grc.security.oidc.SsoPromptAuthorizationRequestResolver;
import com.a9.arbor.grc.security.oidc.TenantClientRegistrationRepository;
import com.a9.arbor.grc.security.ratelimit.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * JWT for API calls + OIDC login for tenant SSO.
 *
 * <p>API traffic stays effectively stateless (JWT). The OAuth2 handshake briefly uses an HTTP
 * session to hold the authorization request ({@code SessionCreationPolicy.IF_REQUIRED}).
 *
 * <p>{@code /api/admin/**} requires SUPER_ADMIN. Tenant SSO entry is {@code /sso/{tenantId}} →
 * {@code /oauth2/authorization/{tenantId}} → Entra → callback → app JWT → SPA.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuditorAccessFilter auditorAccessFilter;
    private final LicenseWriteLockFilter licenseWriteLockFilter;
    private final RateLimitFilter rateLimitFilter;
    private final TenantClientRegistrationRepository clientRegistrationRepository;
    private final OidcLoginSuccessHandler oidcLoginSuccessHandler;
    private final OidcLoginFailureHandler oidcLoginFailureHandler;
    private final SsoPromptAuthorizationRequestResolver ssoPromptAuthorizationRequestResolver;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuditorAccessFilter auditorAccessFilter,
                          LicenseWriteLockFilter licenseWriteLockFilter,
                          RateLimitFilter rateLimitFilter,
                          TenantClientRegistrationRepository clientRegistrationRepository,
                          OidcLoginSuccessHandler oidcLoginSuccessHandler,
                          OidcLoginFailureHandler oidcLoginFailureHandler,
                          SsoPromptAuthorizationRequestResolver ssoPromptAuthorizationRequestResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.auditorAccessFilter = auditorAccessFilter;
        this.licenseWriteLockFilter = licenseWriteLockFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oidcLoginSuccessHandler = oidcLoginSuccessHandler;
        this.oidcLoginFailureHandler = oidcLoginFailureHandler;
        this.ssoPromptAuthorizationRequestResolver = ssoPromptAuthorizationRequestResolver;
    }

    /**
     * Paths that must bypass the security filter chain entirely:
     * <ul>
     *   <li>External audit OTP portal (nested {@code /api/audits/**} matching is unreliable)</li>
     *   <li>{@code /api/health} — Azure App Service health probe (must never 401)</li>
     *   <li>Hashed Vite assets + favicons (binaries; document CSP still applies on HTML routes)</li>
     * </ul>
     * {@code /} and {@code /index.html} stay on the filter chain so CSP matches {@code /signin}.
     * Local Vite (:5173) is unaffected.
     */
    @Bean
    public WebSecurityCustomizer publicPathsBypassSecurityFilters() {
        return web -> web.ignoring().requestMatchers(
                antMatcher("/api/audits/external-access/**"),
                antMatcher("/api/health"),
                antMatcher("/api/webhooks/malware-scan"),
                antMatcher("/assets/**"),
                antMatcher("/favicon.ico"),
                antMatcher("/favicon.svg"),
                antMatcher("/favicon-32.png"),
                antMatcher("/favicon-16.png"),
                antMatcher("/apple-touch-icon.png"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> {})
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "font-src 'self'; "
                                        + "img-src 'self' data: blob:; "
                                        + "connect-src 'self'; "
                                        + "frame-src 'self' blob:; "
                                        + "frame-ancestors 'none'; "
                                        + "base-uri 'self'"))
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()")))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                antMatcher("/api/auth/login"),
                                antMatcher("/api/auth/mfa/verify"),
                                antMatcher("/api/auth/refresh"),
                                antMatcher("/api/auth/logout"),
                                antMatcher("/api/auth/forgot-password"),
                                antMatcher("/api/auth/reset-password"),
                                antMatcher("/api/auth/password-link"),
                                antMatcher("/api/auth/sso/exchange"),
                                antMatcher("/api/auth/tenant/**"),
                                antMatcher("/api/auth/super-admin/login"),
                                antMatcher("/api/health"),
                                antMatcher("/api/webhooks/malware-scan"),
                                antMatcher("/api/audits/external-access/**"),
                                antMatcher("/api/tprm/vendor-access/**"),
                                antMatcher("/sso/**"),
                                antMatcher("/oauth2/**"),
                                antMatcher("/login/oauth2/**"),
                                antMatcher("/"),
                                antMatcher("/index.html"),
                                antMatcher("/assets/**"),
                                antMatcher("/vendor-portal"),
                                antMatcher("/vendor-portal/**"),
                                antMatcher("/external-audit"),
                                antMatcher("/external-audit/**"),
                                antMatcher("/signin"),
                                antMatcher("/signin/**"),
                                antMatcher("/login"),
                                antMatcher("/ops/signin"),
                                antMatcher("/favicon.ico"),
                                antMatcher("/favicon.svg"),
                                antMatcher("/favicon-32.png"),
                                antMatcher("/favicon-16.png"),
                                antMatcher("/apple-touch-icon.png"))
                        .permitAll()
                        .requestMatchers(SpaRoutes::isPublicDocumentGet)
                        .permitAll()
                        .requestMatchers(antMatcher("/actuator/**")).hasRole("SUPER_ADMIN")
                        .requestMatchers(antMatcher("/api/admin/**")).hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))
                .oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(ssoPromptAuthorizationRequestResolver))
                        .successHandler(oidcLoginSuccessHandler)
                        .failureHandler(oidcLoginFailureHandler))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditorAccessFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(licenseWriteLockFilter, AuditorAccessFilter.class);
        return http.build();
    }
}
