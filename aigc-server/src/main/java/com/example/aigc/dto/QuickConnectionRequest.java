package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record QuickConnectionRequest(
        @NotBlank(message = "提供商不能为空") String provider,
        @NotBlank(message = "模型名称不能为空") String modelName,
        @NotBlank(message = "API Key不能为空") String apiKey,
        Boolean enabled
) {
}
