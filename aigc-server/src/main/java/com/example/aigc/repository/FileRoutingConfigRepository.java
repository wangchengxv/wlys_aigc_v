package com.example.aigc.repository;

import com.example.aigc.model.RoutingConfig;
import com.fasterxml.jackson.core.type.TypeReference;

public class FileRoutingConfigRepository implements RoutingConfigRepository {

    private static final String FILE_NAME = "routing-config.json";

    private final JsonFileStorageSupport storageSupport;
    private RoutingConfig config;

    public FileRoutingConfigRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
        this.config = storageSupport.readValue(
                FILE_NAME,
                new TypeReference<RoutingConfig>() {
                },
                RoutingConfig::createDefault
        );
    }

    @Override
    public synchronized RoutingConfig get() {
        return config;
    }

    @Override
    public synchronized RoutingConfig save(RoutingConfig config) {
        this.config = config == null ? RoutingConfig.createDefault() : config;
        storageSupport.writeValue(FILE_NAME, this.config);
        return this.config;
    }
}
