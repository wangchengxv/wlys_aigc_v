package com.example.aigc.dto;

public record StoryboardPlanResponse(
        String assetId,
        String storyboardPlanJson,
        String storyboardTranslationsJson
) {
}
