package dev.portfolio.ratelimit.ratelimit;

import java.time.Duration;
import java.util.Objects;

public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remainingTokens,
        Duration retryAfter) {

    public RateLimitDecision {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (remainingTokens < 0) {
            throw new IllegalArgumentException("remainingTokens must not be negative");
        }
        Objects.requireNonNull(retryAfter, "retryAfter must not be null");
    }
}
