package com.example.aigcspringai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record TextGenerationRequest(
        @NotBlank String providerCode,
        @NotBlank String modelCode,
        @NotEmpty List<@Valid TextMessage> messages,
        boolean stream,
        Double temperature,
        Double topP,
        Integer maxTokens,
        String responseFormat,
        String requestId,
        Long timeoutMs,
        Map<String, Object> extraMetadata
) {
}
