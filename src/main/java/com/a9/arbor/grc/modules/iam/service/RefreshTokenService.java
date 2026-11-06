package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.modules.iam.domain.RefreshToken;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.RefreshTokenRepository;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public static final long REFRESH_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String issueForUser(User user) {
        return issueForUser(user, null);
    }

    @Transactional
    public String issueForUser(User user, String authMethod) {
        String raw = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID();
        RefreshToken row = new RefreshToken();
        row.setUserId(user.getId());
        row.setTokenHash(hash(raw));
        row.setAuthMethod(authMethod);
        row.setExpiresAt(Instant.now().plusSeconds(REFRESH_DAYS * 24 * 3600));
        refreshTokenRepository.save(row);
        return raw;
    }

    public record RotatedSession(User user, String authMethod) {
    }

    @Transactional
    public RotatedSession rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        Instant now = Instant.now();
        RefreshReuse.Decision decision = RefreshReuse.decide(
                now, existing.getExpiresAt(), existing.getRevokedAt(), existing.getRevokedReason());
        if (decision == RefreshReuse.Decision.REUSE_REVOKE_ALL) {
            revokeAllForUser(existing.getUserId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (decision == RefreshReuse.Decision.REJECT) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (decision == RefreshReuse.Decision.ROTATE) {
            existing.setRevokedAt(now);
            existing.setRevokedReason(RefreshReuse.ROTATED);
            refreshTokenRepository.save(existing);
        }
        User user = activeUser(existing);
        return new RotatedSession(user, existing.getAuthMethod());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(row -> {
            if (row.getRevokedAt() == null) {
                row.setRevokedAt(Instant.now());
                row.setRevokedReason(RefreshReuse.LOGOUT);
                refreshTokenRepository.save(row);
            }
        });
    }

    private User activeUser(RefreshToken existing) {
        User user = userRepository.findById(existing.getUserId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User inactive"));
        if ("LOCAL".equals(existing.getAuthMethod()) && !user.isLocalLoginEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return user;
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public void revokeLocalForUser(Long userId) {
        refreshTokenRepository.revokeForUserAndMethod(userId, "LOCAL", Instant.now());
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
