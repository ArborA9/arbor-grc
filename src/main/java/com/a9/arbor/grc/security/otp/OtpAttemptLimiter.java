package com.a9.arbor.grc.security.otp;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks failed OTP attempts per invite token (hashed key). Blocks further redeem attempts after
 * {@code arbor.security.rate-limit.otp-max-failures} within the lockout window.
 */
@Component
public class OtpAttemptLimiter {

    private final int maxFailures;
    private final Duration lockout;

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public OtpAttemptLimiter(ArborSecurityProperties properties) {
        var rl = properties.getRateLimit();
        this.maxFailures = Math.max(1, rl.getOtpMaxFailures());
        this.lockout = Duration.ofMinutes(Math.max(1, rl.getOtpLockoutMinutes()));
    }

    public void assertAllowed(String scopeKey) {
        AttemptWindow window = attempts.get(scopeKey);
        if (window == null) {
            return;
        }
        synchronized (window) {
            window.prune(lockout);
            if (window.failures.get() >= maxFailures) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed OTP attempts. Try again later or request a new code.");
            }
        }
    }

    public void recordFailure(String scopeKey) {
        AttemptWindow window = attempts.computeIfAbsent(scopeKey, k -> new AttemptWindow());
        synchronized (window) {
            window.prune(lockout);
            window.failures.incrementAndGet();
            window.lastFailure = Instant.now();
        }
    }

    public void clear(String scopeKey) {
        attempts.remove(scopeKey);
    }

    private static final class AttemptWindow {
        private final AtomicInteger failures = new AtomicInteger(0);
        private Instant lastFailure = Instant.now();

        void prune(Duration lockout) {
            if (lastFailure.plus(lockout).isBefore(Instant.now())) {
                failures.set(0);
            }
        }
    }
}
