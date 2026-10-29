package dev.portfolio.ratelimit.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class DefaultClientKeyResolverTest {

    private final DefaultClientKeyResolver resolver = new DefaultClientKeyResolver();

    @Test
    void shouldPreferExplicitClientId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DefaultClientKeyResolver.CLIENT_ID_HEADER, "  customer-42  ");
        request.setRemoteAddr("192.0.2.1");

        assertThat(resolver.resolve(request)).isEqualTo("client:customer-42");
    }

    @Test
    void shouldFallBackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");

        assertThat(resolver.resolve(request)).isEqualTo("ip:192.0.2.1");
    }
}
