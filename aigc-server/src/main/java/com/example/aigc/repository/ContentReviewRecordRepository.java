package com.example.aigc.repository;

import com.example.aigc.entity.ContentReviewRecord;

import java.util.List;

public interface ContentReviewRecordRepository {
    ContentReviewRecord save(ContentReviewRecord record);

    List<ContentReviewRecord> findAllByProjectId(String projectId);
}
