package com.example.aigc.repository;

import com.example.aigc.model.ModelConfig;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FileModelConfigRepository implements ModelConfigRepository {

    private static final String FILE_NAME = "models.json";

    private final JsonFileStorageSupport storageSupport;
    private final ConcurrentHashMap<String, ModelConfig> store = new ConcurrentHashMap<>();

    public FileModelConfigRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
        List<ModelConfig> configs = storageSupport.readValue(
                FILE_NAME,
                new TypeReference<List<ModelConfig>>() {
                },
                ArrayList::new
        );
        for (ModelConfig config : configs) {
            store.put(config.getId(), config);
        }
    }

    @Override
    public synchronized ModelConfig save(ModelConfig config) {
        store.put(config.getId(), config);
        persist();
        return config;
    }

    @Override
    public Optional<ModelConfig> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ModelConfig> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(ModelConfig::getCreatedAt, Comparator.nullsLast(Instant::compareTo)))
                .toList();
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public synchronized void deleteById(String id) {
        store.remove(id);
        persist();
    }

    private void persist() {
        storageSupport.writeValue(FILE_NAME, findAll());
    }
}
