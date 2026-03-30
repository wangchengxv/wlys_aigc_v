package com.example.aigc.dto;

import com.example.aigc.enums.GenerateMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GenerateRequest(
        @NotBlank(message = "prompt不能为空")
        @Size(max = 500, message = "prompt最长500字")
        String prompt,

        @NotNull(message = "mode不能为空")
        GenerateMode mode,

        @NotBlank(message = "style不能为空")
        @Size(max = 4096, message = "style最长4096字")
        String style,

        @Pattern(regexp = "^(512x512|768x768|1024x1024)$", message = "imageSize仅支持512x512/768x768/1024x1024")
        String imageSize,

        @Pattern(regexp = "^(short|medium|long)$", message = "textLength仅支持short/medium/long")
        String textLength,

        @Min(value = 1, message = "count最小为1")
        @Max(value = 4, message = "count最大为4")
        Integer count,

        @Size(max = 120, message = "imageModel最长120字")
        String imageModel,

        @Size(max = 120, message = "videoModel最长120字")
        String videoModel
) {
}
