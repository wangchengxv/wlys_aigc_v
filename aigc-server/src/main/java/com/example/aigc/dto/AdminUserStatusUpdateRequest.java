package com.example.aigc.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(
        @NotNull(message = "账号状态不能为空")
        Boolean enabled
) {
}
