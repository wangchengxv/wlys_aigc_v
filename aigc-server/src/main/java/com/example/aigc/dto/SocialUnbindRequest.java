package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialUnbindRequest(
        @NotBlank(message = "provider 不能为空") String provider
) {
}
