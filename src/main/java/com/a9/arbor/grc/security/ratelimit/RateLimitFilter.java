package com.a9.arbor.grc.security.ratelimit;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-IP rate limits on authentication and public OTP redeem endpoints.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final boolean enabled;
    private final int authMaxPerMinute;
    private final int otpRedeemMaxPerMinute;
    private final int tenantSsoDiscoverMaxPerMinute;

    public RateLimitFilter(RateLimitService rateLimitService, ArborSecurityProperties properties) {
        this.rateLimitService = rateLimitService;
        var rl = properties.getRateLimit();
        this.enabled = rl.isEnabled();
        this.authMaxPerMinute = rl.getAuthMaxPerMinute();
        this.otpRedeemMaxPerMinute = rl.getOtpRedeemMaxPerMinute();
        this.tenantSsoDiscoverMaxPerMinute = rl.getTenantSsoDiscoverMaxPerMinute();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        String clientIp = clientIp(request);
        Limit limit = limitFor(path, request.getMethod());
        if (limit != null) {
            String key = limit.bucket + ":" + clientIp;
            if (!rateLimitService.tryConsume(key, limit.maxPerWindow, Duration.ofMinutes(1))) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too many requests. Please wait and try again.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private Limit limitFor(String path, String method) {
        if ("GET".equalsIgnoreCase(method)
                && path.startsWith("/api/auth/tenant/")
                && path.endsWith("/sso")) {
            return new Limit("tenant-sso-discover", tenantSsoDiscoverMaxPerMinute);
        }
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }
        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/mfa/verify")
                || path.startsWith("/api/auth/super-admin/login")) {
            return new Limit("auth", authMaxPerMinute);
        }
        if (path.equals("/api/audits/external-access/redeem")
                || path.equals("/api/tprm/vendor-access/redeem")
                || path.equals("/api/tprm/vendor-access/request-otp")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password")
                || path.equals("/api/auth/password-link")) {
            return new Limit("otp-redeem", otpRedeemMaxPerMinute);
        }
        if (path.equals("/api/auth/sso/exchange")) {
            return new Limit("sso-exchange", authMaxPerMinute);
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Limit(String bucket, int maxPerWindow) {
    }
}
