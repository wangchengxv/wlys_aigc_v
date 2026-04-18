package com.example.aigc.dto;

import java.util.Map;

/**
 * Request DTO for PUT /api/v1/script-projects/{projectId}/model-settings.
 * Sends the full overrides map plus optional project-level defaults.
 * Any key present in {@code overrides} with a blank/null value is treated as "remove override".
 */
public record WorkflowModelSettingsUpdateRequest(
        String defaultTextModel,
        String defaultImageModel,
        String defaultVideoModel,
        String defaultTtsModel,
        String dubbingVoice,
        String dubbingLanguage,
        Double dubbingSpeed,
        Map<String, String> overrides
) {}
