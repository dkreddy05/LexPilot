package com.lexpilot.common.observability;

import com.lexpilot.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to populate SLF4J MDC (Mapped Diagnostic Context) with request and tenant IDs
 * for structured, traceable logging across all downstream components.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String TENANT_ID_KEY = "tenantId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Use incoming request ID if present, otherwise generate a new one
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_KEY, requestId);
        
        // Add header to response for client tracing
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            // TenantContext is typically populated by security filters which run after this,
            // but we add it to MDC here if it's somehow already present, or rely on a 
            // secondary interceptor/filter for the tenant ID specifically. 
            // (In LexPilot, JwtAuthFilter will update the MDC once it extracts the tenant).
            if (TenantContext.getTenantId() != null) {
                MDC.put(TENANT_ID_KEY, TenantContext.getTenantId().toString());
            }

            filterChain.doFilter(request, response);
        } finally {
            // Crucial: clear MDC to prevent leakage in thread pools
            MDC.remove(REQUEST_ID_KEY);
            MDC.remove(TENANT_ID_KEY);
        }
    }
}
