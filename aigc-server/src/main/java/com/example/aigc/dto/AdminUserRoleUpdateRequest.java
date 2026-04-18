package com.example.aigc.dto;

import com.example.aigc.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminUserRoleUpdateRequest(
        @NotNull(message = "角色不能为空")
        UserRole role
) {
}
