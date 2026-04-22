package com.example.aigc.dto;

import java.util.List;

public record BatchReviewSubmissionsResponse(
        int totalRequested,
        int successCount,
        int failedCount,
        List<FailedItem> failedItems
) {
    public record FailedItem(
            String submissionId,
            String reason
    ) {
    }
}