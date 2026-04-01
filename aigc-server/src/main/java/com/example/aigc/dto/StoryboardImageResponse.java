package com.example.aigc.dto;

public record StoryboardImageResponse(
        String assetId,
        String imageFileId,
        String promptText
) {
}
