package com.example.aigc.dto;

public record RewriteScriptPreviewResponse(
        String baseUsed,
        int sourceLength,
        Integer maxOutputChars,
        String rewrittenText
) {
}
