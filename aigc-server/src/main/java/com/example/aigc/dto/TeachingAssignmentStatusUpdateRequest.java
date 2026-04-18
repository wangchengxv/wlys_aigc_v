package com.example.aigc.dto;

import com.example.aigc.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;

public record TeachingAssignmentStatusUpdateRequest(
        @NotNull(message = "作业状态不能为空")
        AssignmentStatus status
) {
}
