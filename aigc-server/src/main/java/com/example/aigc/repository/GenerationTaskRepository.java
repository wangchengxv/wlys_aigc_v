package com.example.aigc.repository;

import com.example.aigc.entity.GenerationTask;
import com.example.aigc.enums.GenerateMode;

import java.util.List;
import java.util.Optional;

public interface GenerationTaskRepository {
    void save(GenerationTask task);

    Optional<GenerationTask> findByTaskId(String taskId);

    List<GenerationTask> page(int page, int pageSize, GenerateMode mode);

    long count(GenerateMode mode);
}
