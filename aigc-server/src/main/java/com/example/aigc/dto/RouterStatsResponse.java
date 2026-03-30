package com.example.aigc.dto;

public record RouterStatsResponse(
        long requestsToday,
        long requestsWeek,
        long requestsMonth,
        long tokensToday,
        long tokensWeek,
        long tokensMonth,
        long totalRequests,
        long totalTokens
) {
}
