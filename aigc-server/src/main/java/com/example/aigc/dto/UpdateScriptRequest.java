package com.example.aigc.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateScriptRequest(
        @Size(max = 80000, message = "完善剧本内容最长80000字")
        String refinedMarkdown,

        Map<String, Object> structuredScript
) {
}
