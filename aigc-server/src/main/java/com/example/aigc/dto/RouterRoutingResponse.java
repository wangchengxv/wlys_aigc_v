package com.example.aigc.dto;

import java.util.List;

public record RouterRoutingResponse(
        String strategy,
        List<String> priorityConnectionIds,
        boolean failoverEnabled,
        int failoverTimeoutSeconds,
        List<TimeScheduleSlotDto> timeSchedule
) {
}
