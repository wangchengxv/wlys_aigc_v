package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseCreateRequest(
        @NotBlank(message = "课程名称不能为空")
        @Size(max = 255, message = "课程名称不能超过255字符")
        String name,
        @Size(max = 128, message = "课程编码不能超过128字符")
        String code,
        String description
) {
}
