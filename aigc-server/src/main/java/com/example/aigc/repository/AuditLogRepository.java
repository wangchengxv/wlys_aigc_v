package com.example.aigc.repository;

import com.example.aigc.entity.AuditLogRecord;

import java.util.List;

public interface AuditLogRepository {
    AuditLogRecord save(AuditLogRecord record);

    List<AuditLogRecord> findRecent();
}
