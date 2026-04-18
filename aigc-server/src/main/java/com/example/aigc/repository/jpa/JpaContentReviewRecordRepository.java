package com.example.aigc.repository.jpa;

import com.example.aigc.entity.ContentReviewRecord;
import com.example.aigc.repository.ContentReviewRecordRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class JpaContentReviewRecordRepository implements ContentReviewRecordRepository {
    private final SpringDataContentReviewRecordRepository repository;

    public JpaContentReviewRecordRepository(SpringDataContentReviewRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public ContentReviewRecord save(ContentReviewRecord record) {
        return repository.save(record);
    }

    @Override
    public List<ContentReviewRecord> findAllByProjectId(String projectId) {
        return repository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
    }
}
