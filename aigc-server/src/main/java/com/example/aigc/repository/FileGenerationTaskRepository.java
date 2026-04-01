package com.example.aigc.repository;

import com.example.aigc.entity.GenerationTask;
import com.example.aigc.enums.GenerateMode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FileGenerationTaskRepository implements GenerationTaskRepository {

    private static final String FILE_NAME = "generation-tasks.json";

    private final JsonFileStorageSupport storageSupport;
    private final ConcurrentHashMap<String, GenerationTask> store = new ConcurrentHashMap<>();

    public FileGenerationTaskRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
        List<GenerationTask> tasks = storageSupport.readValue(
                FILE_NAME,
                new TypeReference<List<GenerationTask>>() {
                },
                ArrayList::new
        );
        for (GenerationTask task : tasks) {
            store.put(task.getTaskId(), task);
        }
    }

    @Override
    public synchronized void save(GenerationTask task) {
        store.put(task.getTaskId(), task);
        persist();
    }

    @Override
    public Optional<GenerationTask> findByTaskId(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    @Override
    public List<GenerationTask> page(int page, int pageSize, GenerateMode mode, String ownerId) {
        List<GenerationTask> filtered = store.values().stream()
                .filter(task -> ownerId == null || ownerId.equals(task.getOwnerId()))
                .filter(task -> mode == null || task.getMode() == mode)
                .sorted(Comparator.comparing(GenerationTask::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(filtered.size(), from + pageSize);
        if (from >= filtered.size()) {
            return List.of();
        }
        return filtered.subList(from, to);
    }

    @Override
    public long count(GenerateMode mode, String ownerId) {
        return store.values().stream()
                .filter(task -> ownerId == null || ownerId.equals(task.getOwnerId()))
                .filter(task -> mode == null || task.getMode() == mode)
                .count();
    }

    @Override
    public synchronized void deleteByTaskId(String taskId) {
        store.remove(taskId);
        persist();
    }

    private void persist() {
        List<GenerationTask> snapshot = store.values().stream()
                .sorted(Comparator.comparing(GenerationTask::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
        storageSupport.writeValue(FILE_NAME, snapshot);
    }
}
