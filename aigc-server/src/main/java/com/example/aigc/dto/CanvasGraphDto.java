package com.example.aigc.dto;

import java.time.Instant;

public record CanvasGraphDto(
        String id,
        String projectId,
        String title,
        Object graph,
        Object viewport,
        Instant createdAt,
        Instant updatedAt
) {
}
