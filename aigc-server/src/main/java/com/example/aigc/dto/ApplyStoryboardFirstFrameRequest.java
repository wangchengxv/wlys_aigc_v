package com.example.aigc.dto;

public record ApplyStoryboardFirstFrameRequest(
        String assetId,
        String mode,
        Integer panelIndex
) {
}
