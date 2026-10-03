package com.cookify.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter: at most {@code limit} hits per key within {@code window}.
 * Good for a single server; a shared store (e.g. Redis) would be needed when scaling out.
 */
public class RateLimiter {

    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Records a hit for {@code key}.
     *
     * @return 0 if allowed, otherwise the number of seconds until the next hit would be allowed
     */
    public long tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long windowStart = now - window.toMillis();
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() <= windowStart) {
                times.pollFirst();
            }
            if (times.size() >= limit) {
                long retryAfterMs = times.peekFirst() + window.toMillis() - now;
                return Math.max(1, (retryAfterMs + 999) / 1000);
            }
            times.addLast(now);
            return 0;
        }
    }

    /** Drops keys with no recent hits so memory stays bounded. */
    public void evictOlderThan(Duration age) {
        long cutoff = clock.millis() - age.toMillis();
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long last = entry.getValue().peekLast();
                return last == null || last < cutoff;
            }
        });
    }
}
