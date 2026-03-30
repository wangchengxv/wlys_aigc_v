package com.example.aigc.repository;

import com.example.aigc.entity.GenerationTask;
import com.example.aigc.enums.GenerateMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGenerationTaskRepository implements GenerationTaskRepository {
    private final ConcurrentHashMap<String, GenerationTask> map = new ConcurrentHashMap<>();

    @Override
    public void save(GenerationTask task) {
        map.put(task.getTaskId(), task);
    }

    @Override
    public Optional<GenerationTask> findByTaskId(String taskId) {
        return Optional.ofNullable(map.get(taskId));
    }

    @Override
    public List<GenerationTask> page(int page, int pageSize, GenerateMode mode) {
        List<GenerationTask> all = new ArrayList<>(map.values());
        List<GenerationTask> filtered = all.stream()
                .filter(task -> mode == null || task.getMode() == mode)
                .sorted(Comparator.comparing(GenerationTask::getCreatedAt).reversed())
                .toList();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(filtered.size(), from + pageSize);
        if (from >= filtered.size()) {
            return List.of();
        }
        return filtered.subList(from, to);
    }

    @Override
    public long count(GenerateMode mode) {
        return map.values().stream().filter(task -> mode == null || task.getMode() == mode).count();
    }

    @Override
    public void deleteByTaskId(String taskId) {
        map.remove(taskId);
    }
}
