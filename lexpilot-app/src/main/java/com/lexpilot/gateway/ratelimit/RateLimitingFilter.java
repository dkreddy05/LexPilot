package com.lexpilot.gateway.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "lexpilot.rate-limiting.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitingFilter extends OncePerRequestFilter {

    public RateLimitingFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // TODO: Implement Bucket4j + Redis rate limiting
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
