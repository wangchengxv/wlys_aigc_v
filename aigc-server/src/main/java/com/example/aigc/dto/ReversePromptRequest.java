package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReversePromptRequest(
        @NotBlank(message = "图片不能为空")
        @Size(max = 4_000_000, message = "图片输入过长")
        String image,

        @Size(max = 120, message = "模型名称过长")
        String model
) {
}
