package com.example.aigc.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminUserBatchStatusUpdateRequest(
        @NotEmpty(message = "用户ID列表不能为空")
        List<String> userIds,
        @NotNull(message = "账号状态不能为空")
        Boolean enabled
) {
}
