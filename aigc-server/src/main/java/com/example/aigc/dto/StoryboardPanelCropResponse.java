package com.example.aigc.dto;

public record StoryboardPanelCropResponse(
        String assetId,
        int panelIndex,
        String imageFileId
) {
}
