package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ConnectionConfigCreateRequest(
        @NotBlank(message = "连接名称不能为空") String name,
        @NotBlank(message = "提供商不能为空") String provider,
        @NotBlank(message = "Base URL不能为空") String baseUrl,
        /** May be blank when provider auth mode is NONE (e.g. Ollama) or Vertex; use AWS secret for Bedrock. */
        String apiKey,
        @NotNull(message = "启用状态不能为空") Boolean enabled,
        /** Provider-specific options (Azure apiVersion, AWS region, Vertex project, etc.). */
        Map<String, Object> metadata
) {
    public ConnectionConfigCreateRequest {
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}