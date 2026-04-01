package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record RewriteScriptApplyRequest(
        @NotBlank(message = "改写结果不能为空")
        String rewrittenText
) {
}
