package com.example.aigc.dto;

public record StoryboardFirstFrameResponse(
        String shotId,
        String mode,
        String assetId,
        Integer panelIndex,
        String imageFileId
) {
}
