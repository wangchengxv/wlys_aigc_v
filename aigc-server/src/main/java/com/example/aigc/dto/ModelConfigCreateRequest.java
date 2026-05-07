package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ModelConfigCreateRequest(
        @NotBlank(message = "模型名称不能为空") String name,
        @NotBlank(message = "提供商不能为空") String provider,
        @NotBlank(message = "模型标识不能为空") String modelName,
        @NotBlank(message = "连接ID不能为空") String connectionId,
        @NotNull(message = "启用状态不能为空") Boolean enabled,
        Map<String, Object> metadata
) {
}