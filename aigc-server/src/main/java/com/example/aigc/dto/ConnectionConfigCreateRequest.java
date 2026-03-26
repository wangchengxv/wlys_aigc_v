package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConnectionConfigCreateRequest(
        @NotBlank(message = "连接名称不能为空") String name,
        @NotBlank(message = "提供商不能为空") String provider,
        @NotBlank(message = "Base URL不能为空") String baseUrl,
        @NotBlank(message = "API Key不能为空") String apiKey,
        @NotNull(message = "启用状态不能为空") Boolean enabled
) {
}