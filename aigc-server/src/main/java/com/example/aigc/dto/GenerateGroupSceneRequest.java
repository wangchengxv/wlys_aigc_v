package com.example.aigc.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GenerateGroupSceneRequest(
        @NotEmpty(message = "至少选择一个角色")
        List<@Size(max = 64) String> characterAssetIds,

        @Size(max = 500) String location,
        @Size(max = 200) String time,
        @Size(max = 500) String atmosphere,

        /** 为 true 时在生成提示词后调用图像模型出一张概念图 */
        Boolean generateImage
) {
}
