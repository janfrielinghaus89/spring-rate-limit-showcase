package dev.portfolio.ratelimit.api;

import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DemoController {

    private final Clock clock;

    public DemoController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok");
    }

    @PostMapping("/echo")
    public EchoResponse echo(@Valid @RequestBody EchoRequest request) {
        return new EchoResponse(request.message(), Instant.now(clock));
    }
}
