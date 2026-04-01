package com.example.aigc.dto;

import java.util.Map;

public record UpdateScriptRequest(
        String refinedMarkdown,

        Map<String, Object> structuredScript
) {
}
