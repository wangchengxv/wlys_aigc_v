package com.example.aigc.repository.jpa;

import com.example.aigc.entity.ReviewRecord;
import com.example.aigc.repository.ReviewRecordRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class JpaReviewRecordRepository implements ReviewRecordRepository {
    private final SpringDataReviewRecordRepository repository;

    public JpaReviewRecordRepository(SpringDataReviewRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReviewRecord save(ReviewRecord record) {
        return repository.save(record);
    }

    @Override
    public List<ReviewRecord> findAllBySubmissionId(String submissionId) {
        return repository.findAllBySubmissionIdOrderByCreatedAtDesc(submissionId);
    }
}
