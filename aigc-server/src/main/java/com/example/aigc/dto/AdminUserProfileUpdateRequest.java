package com.example.aigc.dto;

import jakarta.validation.constraints.Size;

public record AdminUserProfileUpdateRequest(
        @Size(max = 128, message = "显示名称不能超过128字符")
        String displayName,
        String orgUnitId,
        String classroomId
) {
}
