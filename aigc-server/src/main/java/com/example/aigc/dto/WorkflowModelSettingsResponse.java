package com.example.aigc.dto;

import java.util.Map;

/**
 * Response DTO for GET /api/v1/script-projects/{projectId}/model-settings.
 * Contains both the project-level defaults and the per-function overrides map.
 */
public record WorkflowModelSettingsResponse(
        String projectId,
        String defaultTextModel,
        String defaultImageModel,
        String defaultVideoModel,
        String defaultTtsModel,
        String dubbingVoice,
        String dubbingLanguage,
        Double dubbingSpeed,
        Map<String, String> overrides
) {}
