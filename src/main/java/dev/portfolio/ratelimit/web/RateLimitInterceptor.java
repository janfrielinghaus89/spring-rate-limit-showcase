package dev.portfolio.ratelimit.web;

import dev.portfolio.ratelimit.exception.RateLimitExceededException;
import dev.portfolio.ratelimit.ratelimit.ClientKeyResolver;
import dev.portfolio.ratelimit.ratelimit.RateLimitDecision;
import dev.portfolio.ratelimit.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final RateLimiter rateLimiter;
    private final ClientKeyResolver clientKeyResolver;

    public RateLimitInterceptor(
            RateLimiter rateLimiter,
            ClientKeyResolver clientKeyResolver) {
        this.rateLimiter = rateLimiter;
        this.clientKeyResolver = clientKeyResolver;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        String clientKey = clientKeyResolver.resolve(request);
        RateLimitDecision decision = rateLimiter.tryConsume(clientKey);

        response.setHeader(LIMIT_HEADER, Long.toString(decision.limit()));
        response.setHeader(REMAINING_HEADER, Long.toString(decision.remainingTokens()));

        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfter());
        }

        return true;
    }
}
