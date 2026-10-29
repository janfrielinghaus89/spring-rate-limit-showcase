package dev.portfolio.ratelimit.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {

    @Test
    void shouldRejectNonPositiveDurationsAndCapacity() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var invalid = new RateLimitProperties(
                    0, 0, Duration.ZERO, 0, Duration.ofSeconds(-1));
            var violations = validator.validate(invalid);

            assertThat(violations).hasSize(5);
        }
    }

    @Test
    void shouldAcceptPositiveConfiguration() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var valid = new RateLimitProperties(
                    5, 5, Duration.ofMinutes(1), 100, Duration.ofMinutes(5));

            assertThat(validator.validate(valid)).isEmpty();
        }
    }
}
