package com.example.aigc.dto;

import com.example.aigc.enums.UserRole;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(max = 128, message = "显示名称不能超过128字符")
        String displayName,
        UserRole role,
        String orgUnitId,
        String classroomId,
        Boolean enabled,
        @Size(min = 6, message = "重置密码至少6位")
        String password
) {
}
