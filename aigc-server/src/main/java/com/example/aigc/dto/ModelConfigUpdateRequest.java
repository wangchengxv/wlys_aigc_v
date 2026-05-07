package com.example.aigc.dto;

import java.util.Map;

public record ModelConfigUpdateRequest(
        String name,
        String provider,
        String modelName,
        String connectionId,
        Boolean enabled,
        Map<String, Object> metadata
) {
}