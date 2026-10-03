package com.cookify.security.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;

/** Shared limiter instance plus periodic cleanup of idle keys. */
@Configuration
@EnableScheduling
public class RateLimitConfig {

    private RateLimiter limiter;

    @Bean
    public RateLimiter rateLimiter(Clock clock) {
        limiter = new RateLimiter(clock);
        return limiter;
    }

    @Scheduled(fixedDelay = 300_000)
    void evictIdleKeys() {
        if (limiter != null) {
            limiter.evictOlderThan(Duration.ofMinutes(30));
        }
    }
}
