package com.example.aigc.repository;

import com.example.aigc.model.RouterApiKey;

import java.util.List;
import java.util.Optional;

public interface RouterApiKeyRepository {
    RouterApiKey save(RouterApiKey apiKey);

    Optional<RouterApiKey> findById(String id);

    Optional<RouterApiKey> findByKeyValue(String keyValue);

    List<RouterApiKey> findAll();

    void deleteById(String id);
}
