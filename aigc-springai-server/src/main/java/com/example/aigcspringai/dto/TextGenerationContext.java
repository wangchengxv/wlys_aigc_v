package com.example.aigcspringai.dto;

public record TextGenerationContext(
        String scene,
        String tenantId,
        String userId,
        String traceId,
        boolean fallbackEnabled
) {
}
