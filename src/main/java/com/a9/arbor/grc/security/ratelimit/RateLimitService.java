package com.a9.arbor.grc.security.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter (single-node). For multi-instance prod, replace with Redis
 * or API-gateway limits.
 */
@Component
public class RateLimitService {

    private final ConcurrentHashMap<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    /** @return true if the request is allowed */
    public boolean tryConsume(String key, int maxRequests, Duration window) {
        if (maxRequests <= 0) {
            return true;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /** Best-effort cleanup for long-running processes. */
    public void pruneOlderThan(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        windows.entrySet().removeIf(entry -> {
            Deque<Instant> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }
}
