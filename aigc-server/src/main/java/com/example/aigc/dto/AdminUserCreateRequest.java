package com.example.aigc.dto;

import com.example.aigc.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 128, message = "用户名不能超过128字符")
        String username,
        @NotBlank(message = "初始密码不能为空")
        @Size(min = 6, message = "初始密码至少6位")
        String password,
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 128, message = "显示名称不能超过128字符")
        String displayName,
        @NotNull(message = "请选择角色")
        UserRole role,
        String orgUnitId,
        String classroomId,
        Boolean enabled
) {
}
