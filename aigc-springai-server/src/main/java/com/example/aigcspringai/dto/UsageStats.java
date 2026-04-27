package com.example.aigcspringai.dto;

public record UsageStats(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {
    public static UsageStats empty() {
        return new UsageStats(0, 0, 0);
    }
}
