package com.example.aigc.dto;

import java.time.Instant;

public record OperationsDashboardActivityDto(
        String key,
        String action,
        String label,
        String summary,
        String entityType,
        String entityId,
        String link,
        Instant occurredAt
) {
}
