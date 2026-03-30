package com.example.aigc.dto;

public record ModelOptionDetailData(
        String modelName,
        String displayName,
        String provider,
        String capability,
        boolean enabled,
        boolean connectionEnabled
) {
}
