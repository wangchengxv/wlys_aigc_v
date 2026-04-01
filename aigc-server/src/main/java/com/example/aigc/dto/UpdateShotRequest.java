package com.example.aigc.dto;

public record UpdateShotRequest(
        String shotType,
        String cameraMove,
        String emotion,
        Integer targetDurationSec
) {
}

