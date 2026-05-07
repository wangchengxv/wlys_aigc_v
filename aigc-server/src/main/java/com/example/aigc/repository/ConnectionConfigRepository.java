package com.example.aigc.repository;

import com.example.aigc.model.ConnectionConfig;

import java.util.Optional;

public interface ConnectionConfigRepository {
    ConnectionConfig save(ConnectionConfig config);
    Optional<ConnectionConfig> findById(String id);
    java.util.List<ConnectionConfig> findAll();
    boolean existsById(String id);
    void deleteById(String id);
}