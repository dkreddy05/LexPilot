package com.lexpilot.gateway.security;

import com.lexpilot.common.tenant.TenantContext;
import com.lexpilot.gateway.security.entity.ApiKeyEntity;
import com.lexpilot.gateway.security.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "lexpilot.security.enabled", havingValue = "true", matchIfMissing = false)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            sendUnauthorized(response, "Missing API key. Provide the X-Api-Key header.");
            return;
        }

        String hash = hashKey(apiKey);
        Optional<ApiKeyEntity> apiKeyEntityOpt = apiKeyRepository.findByKeyHash(hash);

        if (apiKeyEntityOpt.isEmpty()) {
            sendUnauthorized(response, "Invalid API key.");
            return;
        }

        ApiKeyEntity apiKeyEntity = apiKeyEntityOpt.get();

        if (apiKeyEntity.isRevoked()) {
            sendUnauthorized(response, "API key has been revoked.");
            return;
        }

        // Set multi-tenancy context
        TenantContext.setTenantId(apiKeyEntity.getTenantId());

        try {
            // Set authentication context
            var auth = new UsernamePasswordAuthenticationToken(
                    "api-key-principal",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_API_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up tenant context to prevent leakages across thread pools
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.equals("/actuator/info") || path.equals("/") || path.equals("/error");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    private String hashKey(String plainTextKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainTextKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
