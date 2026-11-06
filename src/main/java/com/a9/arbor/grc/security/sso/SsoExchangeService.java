package com.a9.arbor.grc.security.sso;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.security.SecureTokenUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time SSO exchange codes: avoids placing JWTs in browser URL fragments/history.
 */
@Service
public class SsoExchangeService {

    private final long codeTtlSeconds;
    private final ConcurrentHashMap<String, PendingExchange> pending = new ConcurrentHashMap<>();

    public SsoExchangeService(ArborSecurityProperties properties) {
        this.codeTtlSeconds = Math.max(30, properties.getSsoExchange().getCodeTtlSeconds());
    }

    public String issueCode(String jwt, String tenantId, String email, List<String> roles, long expiryMinutes) {
        String code = SecureTokenUtils.randomToken();
        pending.put(code, new PendingExchange(jwt, tenantId, email, roles, expiryMinutes, Instant.now()));
        return code;
    }

    public LoginResponse redeem(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exchange code is required");
        }
        PendingExchange entry = pending.remove(code.trim());
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired SSO code");
        }
        if (entry.createdAt.plusSeconds(codeTtlSeconds).isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SSO code expired: sign in again");
        }
        return LoginResponse.full(entry.jwt, entry.email, entry.tenantId, entry.roles, entry.expiryMinutes, null, "SSO");
    }

    /** Drop expired codes (scheduled or lazy). */
    public void pruneExpired() {
        Instant cutoff = Instant.now().minusSeconds(codeTtlSeconds);
        pending.entrySet().removeIf(e -> e.getValue().createdAt.isBefore(cutoff));
    }

    private record PendingExchange(
            String jwt,
            String tenantId,
            String email,
            List<String> roles,
            long expiryMinutes,
            Instant createdAt
    ) {
    }
}
