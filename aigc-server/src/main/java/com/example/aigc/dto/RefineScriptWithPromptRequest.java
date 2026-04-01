package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record RefineScriptWithPromptRequest(
        @NotBlank(message = "短提示词不能为空")
        String briefPrompt
) {
}

