package com.example.aigcspringai.dto;

public record TextGenerationStreamChunk(
        String delta,
        boolean isFinal,
        String finishReason
) {
}
