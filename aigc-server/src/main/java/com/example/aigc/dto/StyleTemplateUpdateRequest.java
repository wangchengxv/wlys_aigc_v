package com.example.aigc.dto;

import jakarta.validation.constraints.Size;

public record StyleTemplateUpdateRequest(
        @Size(max = 80, message = "模板名称最长80字")
        String name,

        @Size(max = 80, message = "分类最长80字")
        String category,

        @Size(max = 2000, message = "风格特征最长2000字")
        String traits,

        @Size(max = 20000, message = "完整提示词最长20000字")
        String fullPrompt,

        @Size(max = 120, message = "风格锚点最长120字")
        String styleKey,

        Boolean enabled
) {
}
