package com.example.aigc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentCreateRequest(
        @NotBlank(message = "作业标题不能为空")
        @Size(max = 255, message = "作业标题不能超过255字符")
        String title,
        String brief,
        String styleTemplateId,
        String aspectRatio,
        Integer targetDuration,
        String language,
        @NotNull(message = "最晚提交时间不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime dueAt
) {
}
