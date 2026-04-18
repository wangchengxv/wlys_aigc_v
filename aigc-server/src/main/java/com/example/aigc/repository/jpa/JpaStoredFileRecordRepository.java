package com.example.aigc.repository.jpa;

import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.repository.StoredFileRecordRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class JpaStoredFileRecordRepository implements StoredFileRecordRepository {
    private final SpringDataStoredFileRecordRepository repository;

    public JpaStoredFileRecordRepository(SpringDataStoredFileRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StoredFileRecord> findAll() {
        return repository.findAll();
    }

    @Override
    public List<StoredFileRecord> findAllByProjectId(String projectId) {
        return repository.findAllByProjectId(projectId);
    }

    @Override
    public List<StoredFileRecord> findRecent(int limit) {
        List<StoredFileRecord> rows = repository.findTop200ByOrderByCreatedAtDesc();
        return rows.size() <= limit ? rows : rows.subList(0, limit);
    }
}
