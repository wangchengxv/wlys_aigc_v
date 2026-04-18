package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserPasswordResetRequest(
        @NotBlank(message = "重置密码不能为空")
        @Size(min = 6, message = "重置密码至少6位")
        String password,
        Boolean forcePasswordChange
) {
}
