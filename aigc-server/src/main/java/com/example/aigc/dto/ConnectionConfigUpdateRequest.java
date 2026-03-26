package com.example.aigc.dto;

public record ConnectionConfigUpdateRequest(
        String name,
        String provider,
        String baseUrl,
        String apiKey,
        Boolean enabled
) {
}