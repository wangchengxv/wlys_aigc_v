package com.example.aigc.dto;

import jakarta.validation.constraints.Min;

public record AppendScriptPreviewRequest(
        @Min(value = 1, message = "maxAppendChars 最小为 1")
        Integer maxAppendChars
) {
}

