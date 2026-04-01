package com.example.aigc.dto;

import java.time.Instant;
import java.util.Map;

public record ConnectionConfigResponse(
        String id,
        String name,
        String provider,
        String baseUrl,
        String apiKeyMasked,
        boolean hasApiKey,
        boolean enabled,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
}