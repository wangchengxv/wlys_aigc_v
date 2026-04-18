package com.example.aigc.dto;

import com.example.aigc.enums.OrgUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrgUnitCreateRequest(
        @NotBlank(message = "名称不能为空")
        @Size(max = 128, message = "名称不能超过128字符")
        String name,
        @Size(max = 128, message = "编码不能超过128字符")
        String code,
        @NotNull(message = "请选择组织类型")
        OrgUnitType type,
        String parentUnitId
) {
}
