package com.example.aigc.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminUserBatchLockUpdateRequest(
        @NotEmpty(message = "用户ID列表不能为空")
        List<String> userIds,
        @NotNull(message = "锁定状态不能为空")
        Boolean locked,
        @Size(max = 255, message = "锁定原因不能超过255字符")
        String reason
) {
}
