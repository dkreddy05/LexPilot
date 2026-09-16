package com.lexpilot.gateway.security;

import java.util.UUID;

/**
 * Data extracted from a validated JWT.
 */
public record JwtClaims(
        String subject,
        UUID tenantId,
        String role
) {}
