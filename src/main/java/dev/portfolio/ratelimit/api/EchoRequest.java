package dev.portfolio.ratelimit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EchoRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = 200, message = "message must contain at most 200 characters")
        String message) {
}
