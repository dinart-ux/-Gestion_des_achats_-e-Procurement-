package com.example.backend.Service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/** Instance locale simple; en production multi-instance, externalise vers Redis. */
@Component
public class PasswordResetRateLimiter {
    private static final int LIMIT = 5;
    private static final Duration WINDOW = Duration.ofHours(1);
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    public boolean allow(String key) {
        Instant now = clock.instant();
        ArrayDeque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(now.minus(WINDOW))) bucket.removeFirst();
            if (bucket.size() >= LIMIT) return false;
            bucket.addLast(now);
            return true;
        }
    }
}
