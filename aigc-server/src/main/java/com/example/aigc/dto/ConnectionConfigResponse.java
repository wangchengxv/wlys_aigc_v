package com.example.aigc.dto;

import java.time.Instant;

public record ConnectionConfigResponse(
        String id,
        String name,
        String provider,
        String baseUrl,
        String apiKeyMasked,
        boolean hasApiKey,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}