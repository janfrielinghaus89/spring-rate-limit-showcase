package dev.portfolio.ratelimit.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portfolio.ratelimit.config.RateLimitProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class Bucket4jRateLimiterTest {

    @Test
    void shouldRejectRequestWhenBucketIsExhausted() {
        Bucket4jRateLimiter rateLimiter = createRateLimiter(2);

        RateLimitDecision first = rateLimiter.tryConsume("client-a");
        RateLimitDecision second = rateLimiter.tryConsume("client-a");
        RateLimitDecision third = rateLimiter.tryConsume("client-a");

        assertThat(first.allowed()).isTrue();
        assertThat(first.remainingTokens()).isEqualTo(1);
        assertThat(second.allowed()).isTrue();
        assertThat(second.remainingTokens()).isZero();
        assertThat(third.allowed()).isFalse();
        assertThat(third.remainingTokens()).isZero();
        assertThat(third.retryAfter()).isGreaterThan(Duration.ZERO);
    }

    @Test
    void shouldMaintainIndependentBucketsPerClient() {
        Bucket4jRateLimiter rateLimiter = createRateLimiter(1);

        RateLimitDecision firstClientFirstRequest = rateLimiter.tryConsume("client-a");
        RateLimitDecision firstClientSecondRequest = rateLimiter.tryConsume("client-a");
        RateLimitDecision secondClientFirstRequest = rateLimiter.tryConsume("client-b");

        assertThat(firstClientFirstRequest.allowed()).isTrue();
        assertThat(firstClientSecondRequest.allowed()).isFalse();
        assertThat(secondClientFirstRequest.allowed()).isTrue();
    }

    private static Bucket4jRateLimiter createRateLimiter(long capacity) {
        RateLimitProperties properties = new RateLimitProperties(
                capacity,
                capacity,
                Duration.ofHours(1),
                100,
                Duration.ofMinutes(5));
        return new Bucket4jRateLimiter(properties);
    }
}
