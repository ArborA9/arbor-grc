package com.a9.arbor.grc.security;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

/**
 * Ensures tenant JWTs still belong to an active user and match the current token version
 * (invalidated on disable, role change, or password reset).
 */
@Service
public class JwtSessionValidator {

    private final UserRepository userRepository;
    private final boolean enabled;

    public JwtSessionValidator(UserRepository userRepository, ArborSecurityProperties properties) {
        this.userRepository = userRepository;
        this.enabled = properties.getSession().isValidateTokenVersion();
    }

    public boolean isValidTenantSession(String tenantId, Claims claims) {
        if (!enabled || tenantId == null || tenantId.isBlank()) {
            return true;
        }
        String email = claims.getSubject();
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .map(u -> tokenVersionFrom(claims) == u.getTokenVersion() && localStillAllowed(claims, u))
                .orElse(false);
    }

    /** A LOCAL access token dies when local login is turned off. Tokens without the claim stay valid until expiry. */
    private static boolean localStillAllowed(Claims claims, User user) {
        String method = claims.get(JwtService.CLAIM_AUTH_METHOD, String.class);
        if (!"LOCAL".equals(method)) {
            return true;
        }
        return user.isLocalLoginEnabled();
    }

    private static int tokenVersionFrom(Claims claims) {
        Integer tv = claims.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class);
        return tv == null ? 0 : tv;
    }
}
