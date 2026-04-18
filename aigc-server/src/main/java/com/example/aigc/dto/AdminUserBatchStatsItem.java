package com.example.aigc.dto;

import java.util.List;

public record AdminUserBatchStatsItem(
        String action,
        String operatorUserId,
        String operatorUserName,
        String createdAt,
        int total,
        int success,
        int failed,
        List<String> failedUserIds
) {
}
