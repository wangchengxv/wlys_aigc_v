package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefineScriptWithPromptRequest(
        @NotBlank(message = "短提示词不能为空")
        @Size(max = 800, message = "短提示词最长800字")
        String briefPrompt
) {
}

