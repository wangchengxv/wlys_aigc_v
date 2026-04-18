package com.example.aigc.dto;

public record OperationsDashboardMetricDto(
        String key,
        String label,
        long value,
        String entityType,
        String link,
        String summary
) {
}
