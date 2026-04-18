package com.example.aigc.repository;

import com.example.aigc.entity.StoredFileRecord;

import java.util.List;

public interface StoredFileRecordRepository {
    List<StoredFileRecord> findAll();

    List<StoredFileRecord> findAllByProjectId(String projectId);

    List<StoredFileRecord> findRecent(int limit);
}
