package com.example.aigc.dto;

public record AppendScriptPreviewResponse(
        String baseUsed,
        int existingLength,
        int maxAppendChars,
        String appendText
) {
}

