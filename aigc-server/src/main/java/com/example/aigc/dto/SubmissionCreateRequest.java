package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmissionCreateRequest(
        @NotBlank(message = "projectId不能为空")
        String projectId,
        String note
) {
}
