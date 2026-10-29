package dev.portfolio.ratelimit.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultClientKeyResolver implements ClientKeyResolver {

    static final String CLIENT_ID_HEADER = "X-Client-Id";

    @Override
    public String resolve(HttpServletRequest request) {
        String clientId = request.getHeader(CLIENT_ID_HEADER);
        if (StringUtils.hasText(clientId)) {
            return "client:" + clientId.trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
