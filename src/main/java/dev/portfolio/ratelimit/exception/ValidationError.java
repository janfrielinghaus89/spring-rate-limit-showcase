package dev.portfolio.ratelimit.exception;

public record ValidationError(String field, String message) {
}
