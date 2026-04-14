package com.example.aigc.dto;

import jakarta.validation.constraints.NotNull;

public record SaveCanvasRequest(
        String id,
        String projectId,
        String title,
        @NotNull Object graph,
        Object viewport
) {
}
