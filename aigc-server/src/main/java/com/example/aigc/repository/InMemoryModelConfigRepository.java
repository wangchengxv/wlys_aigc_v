package com.example.aigc.repository;

import com.example.aigc.model.ModelConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryModelConfigRepository implements ModelConfigRepository {

    private final Map<String, ModelConfig> store = new HashMap<>();

    @Override
    public ModelConfig save(ModelConfig config) {
        store.put(config.getId(), config);
        return config;
    }

    @Override
    public Optional<ModelConfig> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ModelConfig> findAll() {
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