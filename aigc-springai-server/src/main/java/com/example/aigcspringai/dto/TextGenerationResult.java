package com.example.aigcspringai.dto;

public record TextGenerationResult(
        String text,
        String finishReason,
        UsageStats usage,
        String provider,
        String model,
        String rawResponseRef,
        boolean fallbackOccurred
) {
}
