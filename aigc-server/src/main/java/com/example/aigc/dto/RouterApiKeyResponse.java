package com.example.aigc.dto;

import java.time.Instant;

public record RouterApiKeyResponse(
        String id,
        String name,
        String key,
        String maskedKey,
        boolean active,
        Instant createdAt,
        Instant lastUsedAt
) {
}
