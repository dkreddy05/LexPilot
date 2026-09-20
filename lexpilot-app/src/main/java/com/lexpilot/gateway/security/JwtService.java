package com.lexpilot.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${lexpilot.jwt.secret:change-me-in-production-minimum-32-chars}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtClaims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "USER";
        }
        
        String tenantStr = claims.get("tenantId", String.class);
        UUID tenantId = tenantStr != null ? UUID.fromString(tenantStr) : null;

        return new JwtClaims(subject, tenantId, role);
    }

    public String generateToken(String subject, UUID tenantId, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("tenantId", tenantId != null ? tenantId.toString() : null)
                .claim("role", role)
                .signWith(key)
                .compact();
    }
}
