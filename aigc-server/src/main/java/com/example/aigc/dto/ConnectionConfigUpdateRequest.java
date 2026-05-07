package com.example.aigc.dto;

import java.util.Map;

public record ConnectionConfigUpdateRequest(
        String name,
        String provider,
        String baseUrl,
        String apiKey,
        Boolean enabled,
        Map<String, Object> metadata
) {
}