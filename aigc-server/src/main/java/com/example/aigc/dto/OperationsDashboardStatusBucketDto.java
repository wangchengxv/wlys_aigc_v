package com.example.aigc.dto;

public record OperationsDashboardStatusBucketDto(
        String key,
        String label,
        long count,
        String entityType,
        String link,
        String summary
) {
}
