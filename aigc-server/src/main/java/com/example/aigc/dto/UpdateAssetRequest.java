package com.example.aigc.dto;

import java.util.List;
import java.util.Map;

public record UpdateAssetRequest(
        String name,

        String description,

        List<String> tags,

        String promptDraft,

        Map<String, Object> metadata
) {
}
