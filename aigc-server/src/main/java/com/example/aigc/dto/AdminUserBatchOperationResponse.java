package com.example.aigc.dto;

import java.util.List;

public record AdminUserBatchOperationResponse(
        int total,
        int success,
        int failed,
        List<String> failedUserIds
) {
}
