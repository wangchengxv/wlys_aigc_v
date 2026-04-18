package com.example.aigc.repository;

import com.example.aigc.entity.ReviewRecord;

import java.util.List;

public interface ReviewRecordRepository {
    ReviewRecord save(ReviewRecord record);

    List<ReviewRecord> findAllBySubmissionId(String submissionId);
}
