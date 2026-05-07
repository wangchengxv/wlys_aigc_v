package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record RouterApiKeyCreateRequest(
        @NotBlank(message = "名称不能为空") String name
) {
}
