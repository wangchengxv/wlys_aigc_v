package com.example.aigc.repository;

import com.example.aigc.model.ConnectionConfig;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FileConnectionConfigRepository implements ConnectionConfigRepository {

    private static final String FILE_NAME = "connections.json";

    private final JsonFileStorageSupport storageSupport;
    private final ConcurrentHashMap<String, ConnectionConfig> store = new ConcurrentHashMap<>();

    public FileConnectionConfigRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
        List<ConnectionConfig> configs = storageSupport.readValue(
                FILE_NAME,
                new TypeReference<List<ConnectionConfig>>() {
                },
                ArrayList::new
        );
        for (ConnectionConfig config : configs) {
            store.put(config.getId(), config);
        }
    }

    @Override
    public synchronized ConnectionConfig save(ConnectionConfig config) {
        store.put(config.getId(), config);
        persist();
        return config;
    }

    @Override
    public Optional<ConnectionConfig> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ConnectionConfig> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(ConnectionConfig::getCreatedAt, Comparator.nullsLast(Instant::compareTo)))
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
