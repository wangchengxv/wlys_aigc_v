package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record StoryboardRewriteRequest(
        @NotBlank(message = "改写指令不能为空")
        String instruction
) {
}
