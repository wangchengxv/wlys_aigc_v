package com.example.aigc.dto;

import java.time.Instant;

public record RouterRequestLogResponse(
        String id,
        Instant timestamp,
        String routerApiKeyId,
        String connectionId,
        String connectionName,
        String provider,
        String model,
        String requestFormat,
        String status,
        int durationMs,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        String errorMessage
) {
}
