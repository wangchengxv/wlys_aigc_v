package com.example.aigc.dto;

import com.example.aigc.entity.ContentReviewRecord;
import com.example.aigc.enums.ContentReviewStatus;

import java.time.Instant;
import java.util.List;

public record ContentReviewStatusResponse(
        String projectId,
        ContentReviewStatus status,
        boolean exportPackageReady,
        String currentReviewId,
        Integer resubmitCount,
        String latestReviewComment,
        Instant reviewSubmittedAt,
        Instant reviewedAt,
        String reviewerUserId,
        String reviewerUserName,
        boolean canSubmit,
        boolean canProcess,
        List<ContentReviewRecord> records
) {
}
