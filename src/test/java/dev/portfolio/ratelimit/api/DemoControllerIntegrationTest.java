package dev.portfolio.ratelimit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.rate-limit.capacity=20",
        "app.rate-limit.refill-tokens=20",
        "app.rate-limit.refill-period=1m"
})
@AutoConfigureMockMvc
class DemoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEchoValidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/echo")
                        .header("X-Client-Id", "echo-valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello portfolio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("hello portfolio"))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @Test
    void shouldReturnStructuredProblemForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/echo")
                        .header("X-Client-Id", "echo-malformed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request body"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/echo"));
    }

    @Test
    void shouldReturnStructuredValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/echo")
                        .header("X-Client-Id", "echo-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/echo"))
                .andExpect(jsonPath("$.errors[0].field").value("message"));
    }
}
