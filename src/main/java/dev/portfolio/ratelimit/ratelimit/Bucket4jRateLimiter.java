package dev.portfolio.ratelimit.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.portfolio.ratelimit.config.RateLimitProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Bucket4jRateLimiter implements RateLimiter {

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;
    private final TimeMeter timeMeter;

    @Autowired
    public Bucket4jRateLimiter(RateLimitProperties properties) {
        this(properties, TimeMeter.SYSTEM_MILLISECONDS);
    }

    Bucket4jRateLimiter(RateLimitProperties properties, TimeMeter timeMeter) {
        this.properties = Objects.requireNonNull(properties);
        this.timeMeter = Objects.requireNonNull(timeMeter);
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.maxTrackedClients())
                .expireAfterAccess(properties.expireAfterAccess())
                .build();
    }

    @Override
    public RateLimitDecision tryConsume(String clientKey) {
        Bucket bucket = buckets.get(clientKey, ignored -> createBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        Duration retryAfter = probe.isConsumed()
                ? Duration.ZERO
                : Duration.ofNanos(probe.getNanosToWaitForRefill());

        return new RateLimitDecision(
                probe.isConsumed(),
                properties.capacity(),
                probe.getRemainingTokens(),
                retryAfter);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limit -> limit
                        .capacity(properties.capacity())
                        .refillGreedy(properties.refillTokens(), properties.refillPeriod()))
                .build();
    }
}
