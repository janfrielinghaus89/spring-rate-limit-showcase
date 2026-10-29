package dev.portfolio.ratelimit.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.rate-limit.capacity=2",
        "app.rate-limit.refill-tokens=2",
        "app.rate-limit.refill-period=1h",
        "app.rate-limit.max-tracked-clients=100",
        "app.rate-limit.expire-after-access=5m"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnRateLimitHeadersAndProblemDetailWhenLimitIsExceeded() throws Exception {
        String clientId = "integration-client";

        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(header().string(RateLimitInterceptor.LIMIT_HEADER, "2"))
                .andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, "1"));

        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, "0"));

        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", clientId))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, "0"))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Rate limit exceeded"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.instance").value("/api/v1/ping"))
                .andExpect(jsonPath("$.path").value("/api/v1/ping"))
                .andExpect(jsonPath("$.retryAfterSeconds", greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldKeepClientsIsolated() throws Exception {
        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", "client-one"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", "client-one"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", "client-one"))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/v1/ping").header("X-Client-Id", "client-two"))
                .andExpect(status().isOk());
    }
}
