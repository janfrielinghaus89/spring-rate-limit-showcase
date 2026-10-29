package dev.portfolio.ratelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RateLimitShowcaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateLimitShowcaseApplication.class, args);
    }
}
