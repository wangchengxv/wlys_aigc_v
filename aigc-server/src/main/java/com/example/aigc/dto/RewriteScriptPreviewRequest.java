package com.example.aigc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RewriteScriptPreviewRequest(
        @NotBlank(message = "改写要求不能为空")
        String rewriteInstruction,
        String targetStyle,
        @Min(value = 1, message = "字数上限最小 1")
        Integer maxOutputChars,
        @Size(max = 32, message = "语言标识最长 32 字")
        String language
) {
}
