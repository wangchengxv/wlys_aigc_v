package com.example.aigc.repository;

import com.example.aigc.model.ModelConfig;

import java.util.List;
import java.util.Optional;

public interface ModelConfigRepository {
    ModelConfig save(ModelConfig config);
    Optional<ModelConfig> findById(String id);
    List<ModelConfig> findAll();
    boolean existsById(String id);
    void deleteById(String id);
}