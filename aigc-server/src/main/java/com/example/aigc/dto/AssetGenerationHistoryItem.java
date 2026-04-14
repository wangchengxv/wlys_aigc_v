package com.example.aigc.dto;

import java.time.Instant;

public record AssetGenerationHistoryItem(
        long id,
        String projectId,
        String assetType,
        String referenceId,
        String fileId,
        String promptText,
        String modelName,
        String generationParamsJson,
        Instant createdAt
) {
}
