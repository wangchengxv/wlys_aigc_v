package com.example.aigc.dto;

import java.util.List;

public record AdminUserBatchStatsResponse(
        List<AdminUserBatchStatsItem> items,
        int totalRequested,
        int totalSuccess,
        int totalFailed
) {
}
