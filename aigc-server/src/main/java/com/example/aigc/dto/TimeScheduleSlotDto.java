package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;

public record TimeScheduleSlotDto(
        @NotBlank(message = "开始时间不能为空") String start,
        @NotBlank(message = "结束时间不能为空") String end,
        @NotBlank(message = "连接ID不能为空") String connectionId
) {
}
