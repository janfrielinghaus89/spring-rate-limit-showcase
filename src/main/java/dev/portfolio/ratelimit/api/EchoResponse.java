package dev.portfolio.ratelimit.api;

import java.time.Instant;

public record EchoResponse(String message, Instant timestamp) {
}
