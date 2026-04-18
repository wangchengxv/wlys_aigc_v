package com.example.aigc.repository.jpa;

import com.example.aigc.entity.AuditLogRecord;
import com.example.aigc.repository.AuditLogRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class JpaAuditLogRepository implements AuditLogRepository {
    private final SpringDataAuditLogRepository repository;

    public JpaAuditLogRepository(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        return repository.save(record);
    }

    @Override
    public List<AuditLogRecord> findRecent() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }
}
