package com.example.aigc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserLockUpdateRequest(
        @NotNull(message = "锁定状态不能为空")
        Boolean locked,
        @Size(max = 255, message = "锁定原因不能超过255字符")
        String reason
) {
}
