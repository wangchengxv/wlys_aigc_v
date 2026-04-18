package com.example.aigc.dto;

import java.time.Instant;
import java.util.List;

public record OperationsDashboardResponse(
        Instant generatedAt,
        List<OperationsDashboardMetricDto> overviewCards,
        List<OperationsDashboardStatusBucketDto> statusDistribution,
        List<OperationsDashboardActivityDto> recentActivities
) {
}
