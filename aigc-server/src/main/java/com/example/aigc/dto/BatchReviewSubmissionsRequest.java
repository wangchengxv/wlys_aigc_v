package com.example.aigc.dto;

import com.example.aigc.enums.SubmissionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchReviewSubmissionsRequest(
        @NotEmpty(message = "提交ID列表不能为空")
        List<String> submissionIds,
        @NotEmpty(message = "评审状态不能为空")
        SubmissionStatus status,
        @Min(value = 0, message = "评分不能低于0")
        @Max(value = 100, message = "评分不能高于100")
        Integer score,
        String comment
) {
}