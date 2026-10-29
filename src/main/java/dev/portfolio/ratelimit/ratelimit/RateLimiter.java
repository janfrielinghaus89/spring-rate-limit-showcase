package dev.portfolio.ratelimit.ratelimit;

public interface RateLimiter {

    RateLimitDecision tryConsume(String clientKey);
}
