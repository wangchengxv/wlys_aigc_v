package com.example.aigc.dto;

import java.util.List;
import java.util.Map;

public record AssignmentStatsResponse(
        String assignmentId,
        String assignmentTitle,
        int totalStudents,
        int submittedCount,
        int pendingReviewCount,
        int reviewedCount,
        int returnedCount,
        Double averageScore,
        int maxScore,
        int minScore,
        Map<Integer, Integer> scoreDistribution,
        List<ScoreBucket> scoreBuckets
) {
    public record ScoreBucket(
            String label,
            int minScore,
            int maxScore,
            int count
    ) {
    }
}