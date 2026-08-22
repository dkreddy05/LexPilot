package com.lexpilot.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the security filter chain when {@code lexpilot.security.enabled=true}.
 * <p>
 * Covers: missing API key → 401, wrong API key → 401, correct key → non-401,
 * rate limit exceeded → 429, and actuator/health bypasses auth entirely.
 * <p>
 * Uses the {@code mockdb} profile (H2 in-memory) to avoid Testcontainers overhead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("mockdb")
@TestPropertySource(properties = {
        "lexpilot.security.enabled=true",
        "lexpilot.api-key=test-secret-key",
        "lexpilot.rate-limiting.requests-per-minute=5",
        "lexpilot.rate-limiting.requests-per-day=1000",
        "lexpilot.llm.api-key=test-key",
        "lexpilot.embedding-service.base-url=http://localhost:19999",
        "lexpilot.ingestion.upload-dir=./test-uploads",
        "lexpilot.ingestion.max-file-size-mb=20"
})
class SecurityConfigTest {

    private static final String VALID_API_KEY = "test-secret-key";
    private static final String API_ENDPOINT = "/api/v1/query";
    private static final String ACTUATOR_ENDPOINT = "/actuator/health";

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // API Key Authentication
    // -------------------------------------------------------------------------

    @Test
    void request_withoutApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(API_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Missing API key. Provide the X-Api-Key header."));
    }

    @Test
    void request_withWrongApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(API_ENDPOINT)
                        .header("X-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid API key."));
    }

    @Test
    void request_withCorrectApiKey_shouldNotReturn401() throws Exception {
        // The request may fail with a downstream error (e.g., 500 because services aren't real),
        // but it should NOT be 401 — proving the auth filter accepted the key.
        mockMvc.perform(post(API_ENDPOINT)
                        .header("X-Api-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Expected non-401 status but got 401";
                    assert status != 403 : "Expected non-403 status but got 403";
                });
    }

    // -------------------------------------------------------------------------
    // Rate Limiting
    // -------------------------------------------------------------------------

    @Test
    void request_exceedingRateLimit_shouldReturn429() throws Exception {
        // Rate limit is set to 5 requests per minute in @TestPropertySource.
        // Exhaust the bucket, then the next request should be 429.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(API_ENDPOINT)
                    .header("X-Api-Key", VALID_API_KEY)
                    .header("X-Forwarded-For", "192.0.2.99")  // unique IP to isolate from other tests
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"test\"}"));
        }

        // 6th request should be throttled
        mockMvc.perform(post(API_ENDPOINT)
                        .header("X-Api-Key", VALID_API_KEY)
                        .header("X-Forwarded-For", "192.0.2.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    // -------------------------------------------------------------------------
    // Public Endpoint Bypass
    // -------------------------------------------------------------------------

    @Test
    void actuatorHealth_shouldBypassAuth() throws Exception {
        mockMvc.perform(get(ACTUATOR_ENDPOINT))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Actuator health should not require auth, but got 401";
                    assert status != 403 : "Actuator health should not require auth, but got 403";
                });
    }

    @Test
    void rootPath_shouldBypassAuth() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Root path should not require auth, but got 401";
                    assert status != 403 : "Root path should not require auth, but got 403";
                });
    }
}
