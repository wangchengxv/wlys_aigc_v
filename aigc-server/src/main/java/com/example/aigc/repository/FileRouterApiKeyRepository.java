package com.example.aigc.repository;

import com.example.aigc.model.RouterApiKey;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FileRouterApiKeyRepository implements RouterApiKeyRepository {

    private static final String FILE_NAME = "router-api-keys.json";

    private final JsonFileStorageSupport storageSupport;
    private final ConcurrentHashMap<String, RouterApiKey> store = new ConcurrentHashMap<>();

    public FileRouterApiKeyRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
        List<RouterApiKey> apiKeys = storageSupport.readValue(
                FILE_NAME,
                new TypeReference<List<RouterApiKey>>() {
                },
                ArrayList::new
        );
        for (RouterApiKey apiKey : apiKeys) {
            store.put(apiKey.getId(), apiKey);
        }
    }

    @Override
    public synchronized RouterApiKey save(RouterApiKey apiKey) {
        store.put(apiKey.getId(), apiKey);
        persist();
        return apiKey;
    }

    @Override
    public Optional<RouterApiKey> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<RouterApiKey> findByKeyValue(String keyValue) {
        return store.values().stream()
                .filter(apiKey -> keyValue != null && keyValue.equals(apiKey.getKeyValue()))
                .findFirst();
    }

    @Override
    public List<RouterApiKey> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(RouterApiKey::getCreatedAt, Comparator.nullsLast(Instant::compareTo)).reversed())
                .toList();
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
