package com.example.aigc.repository;

import com.example.aigc.model.ConnectionConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryConnectionConfigRepository implements ConnectionConfigRepository {

    private final Map<String, ConnectionConfig> store = new HashMap<>();

    @Override
    public ConnectionConfig save(ConnectionConfig config) {
        store.put(config.getId(), config);
        return config;
    }

    @Override
    public Optional<ConnectionConfig> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ConnectionConfig> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}