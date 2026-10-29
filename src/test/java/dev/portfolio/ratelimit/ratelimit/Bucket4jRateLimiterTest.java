package dev.portfolio.ratelimit.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portfolio.ratelimit.config.RateLimitProperties;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
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

    @Test
    void shouldRefillTokensWithoutWaitingForWallClockTime() {
        ManualTimeMeter time = new ManualTimeMeter();
        RateLimitProperties properties = properties(2, Duration.ofSeconds(10));
        Bucket4jRateLimiter limiter = new Bucket4jRateLimiter(properties, time);

        assertThat(limiter.tryConsume("refill").allowed()).isTrue();
        assertThat(limiter.tryConsume("refill").allowed()).isTrue();
        assertThat(limiter.tryConsume("refill").allowed()).isFalse();

        time.advance(Duration.ofSeconds(5));
        RateLimitDecision halfway = limiter.tryConsume("refill");
        assertThat(halfway.allowed()).isTrue();
        assertThat(halfway.remainingTokens()).isZero();

        time.advance(Duration.ofSeconds(10));
        assertThat(limiter.tryConsume("refill").remainingTokens()).isEqualTo(1);
    }

    @Test
    void shouldNeverAllowMoreRequestsThanCapacityUnderConcurrency() throws Exception {
        int attempts = 40;
        int capacity = 5;
        Bucket4jRateLimiter limiter = new Bucket4jRateLimiter(
                properties(capacity, Duration.ofHours(1)), new ManualTimeMeter());
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
            var futures = IntStream.range(0, attempts)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Concurrent test did not start");
                        }
                        if (limiter.tryConsume("shared-client").allowed()) {
                            allowed.incrementAndGet();
                        }
                        return true;
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Boolean> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            start.countDown();
        }
        assertThat(allowed.get()).isEqualTo(capacity);
    }

    private static final class ManualTimeMeter implements TimeMeter {

        private final AtomicLong currentNanos = new AtomicLong(Duration.ofDays(1).toNanos());

        @Override
        public long currentTimeNanos() {
            return currentNanos.get();
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }

        void advance(Duration duration) {
            currentNanos.addAndGet(duration.toNanos());
        }
    }

    private static Bucket4jRateLimiter createRateLimiter(long capacity) {
        return new Bucket4jRateLimiter(properties(capacity, Duration.ofHours(1)));
    }

    private static RateLimitProperties properties(long capacity, Duration refillPeriod) {
        RateLimitProperties properties = new RateLimitProperties(
                capacity,
                capacity,
                refillPeriod,
                100,
                Duration.ofMinutes(5));
        return properties;
    }
}
