package com.example.aigc.dto;

import java.util.List;

public record RouterRoutingUpdateRequest(
        String strategy,
        List<String> priorityConnectionIds,
        Boolean failoverEnabled,
        Integer failoverTimeoutSeconds,
        List<TimeScheduleSlotDto> timeSchedule
) {
}
