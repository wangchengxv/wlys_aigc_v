package com.example.aigc.service;

import java.util.List;
import java.util.Map;

/**
 * Reusable text-to-image capability entry for different business workflows.
 */
public interface ImageGenerationCapabilityService {

    ImageGenerationResult generateImages(
            String prompt,
            int count,
            String requestedModel,
            Map<String, Object> advancedMedia,
            boolean strictConfiguredModel
    );

    record ImageGenerationResult(
            String modelName,
            List<String> results,
            String modelSource,
            String matchedBy,
            String rejectReason
    ) {
    }
}
