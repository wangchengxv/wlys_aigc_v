package com.example.aigcspringai.dto;

import jakarta.validation.constraints.NotBlank;

public record TextMessage(
        @NotBlank String role,
        @NotBlank String content
) {
}
