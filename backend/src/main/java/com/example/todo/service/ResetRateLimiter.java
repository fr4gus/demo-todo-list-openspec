package com.example.todo.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Fixed-window counter in Redis: at most {@value #LIMIT} reset requests per email per hour. */
@Component
public class ResetRateLimiter {

    static final int LIMIT = 5;
    static final Duration WINDOW = Duration.ofHours(1);
    private static final String KEY_PREFIX = "rate:password-reset:";

    private final StringRedisTemplate redis;

    public ResetRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Counts this attempt; returns false once the email has exceeded the limit within the window. */
    public boolean tryAcquire(String normalizedEmail) {
        String key = KEY_PREFIX + normalizedEmail;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
        return count != null && count <= LIMIT;
    }
}
