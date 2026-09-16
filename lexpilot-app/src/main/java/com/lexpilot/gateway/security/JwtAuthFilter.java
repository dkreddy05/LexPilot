package com.lexpilot.gateway.security;

import com.lexpilot.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.lexpilot.common.config.AppConfig;
// Note: In a real app we'd use io.jsonwebtoken (jjwt) or nimbus-jose-jwt.
// For this plan, we assume a simple JWT parser is implemented or added via deps.

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter that intercepts incoming requests and extracts JWT Bearer tokens.
 * Validates the token and sets the Spring SecurityContext and LexPilot TenantContext.
 * <p>
 * If no Bearer token is found, it falls through, allowing the downstream
 * ApiKeyAuthFilter to attempt authentication (for backward compatibility
 * with system integrations).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // After RequestTraceFilter, Before ApiKeyAuthFilter
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final boolean securityEnabled;
    private final JwtService jwtService;

    public JwtAuthFilter(@Value("${lexpilot.security.enabled:true}") boolean securityEnabled, 
                         JwtService jwtService) {
        this.securityEnabled = securityEnabled;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
                                        
        // If security is disabled globally, skip
        if (!securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            try {
                // Parse JWT (throws exception if invalid/expired)
                JwtClaims claims = jwtService.parseToken(token);

                // Set Security Context
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.subject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Set Tenant Context and MDC
                if (claims.tenantId() != null) {
                    TenantContext.setTenantId(claims.tenantId());
                    MDC.put("tenantId", claims.tenantId().toString());
                    MDC.put("userId", claims.subject());
                }

                log.debug("Authenticated user {} (role: {}, tenant: {})",
                        claims.subject(), claims.role(), claims.tenantId());

            } catch (Exception e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                // Don't fail immediately; let ApiKeyAuthFilter or SecurityConfig handle unauthorized
            }
        }

        filterChain.doFilter(request, response);
    }
}
