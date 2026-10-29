package dev.portfolio.ratelimit.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Min(1) long capacity,
        @Min(1) long refillTokens,
        @NotNull Duration refillPeriod,
        @Min(1) long maxTrackedClients,
        @NotNull Duration expireAfterAccess) {

    @AssertTrue(message = "refillPeriod must be positive")
    public boolean isRefillPeriodPositive() {
        return refillPeriod != null && !refillPeriod.isNegative() && !refillPeriod.isZero();
    }

    @AssertTrue(message = "expireAfterAccess must be positive")
    public boolean isExpireAfterAccessPositive() {
        return expireAfterAccess != null && !expireAfterAccess.isNegative() && !expireAfterAccess.isZero();
    }
}
