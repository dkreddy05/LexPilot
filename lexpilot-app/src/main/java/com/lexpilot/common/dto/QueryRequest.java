package com.lexpilot.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(
        @NotBlank(message = "Query text must not be blank")
        @Size(max = 2000, message = "Query text must not exceed 2000 characters")
        String query,

        String domain,

        String sessionId
) {}
