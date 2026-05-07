package com.example.aigc.dto;

import java.time.Instant;
import java.util.Map;

public record ModelConfigResponse(
        String id,
        String name,
        String provider,
        String modelName,
        String connectionId,
        boolean enabled,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
}