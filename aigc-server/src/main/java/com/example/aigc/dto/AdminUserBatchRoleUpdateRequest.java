package com.example.aigc.dto;

import com.example.aigc.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminUserBatchRoleUpdateRequest(
        @NotEmpty(message = "用户ID列表不能为空")
        List<String> userIds,
        @NotNull(message = "角色不能为空")
        UserRole role
) {
}
