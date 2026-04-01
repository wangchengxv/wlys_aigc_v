package com.example.aigc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScriptProjectCreateRequest(
        @NotBlank(message = "项目名称不能为空")
        @Size(max = 80, message = "项目名称最长80字")
        String name,

        @NotBlank(message = "剧本文本不能为空")
        @Size(max = 50000, message = "剧本文本最长50000字")
        String sourceText,

        @Size(max = 20000, message = "视觉风格最长20000字")
        String visualStyle,

        @Size(max = 20, message = "视频比例最长20字")
        String aspectRatio,

        @Min(value = 1, message = "目标时长最小为1秒")
        @Max(value = 600, message = "目标时长最大为600秒")
        Integer targetDuration,

        @Size(max = 20, message = "语言最长20字")
        String language,

        @Size(max = 120, message = "文本模型最长120字")
        String explicitTextModel,

        @Size(max = 120, message = "图片模型最长120字")
        String explicitImageModel,

        @Size(max = 120, message = "视频模型最长120字")
        String explicitVideoModel
) {
}
