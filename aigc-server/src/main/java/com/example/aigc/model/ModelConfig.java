package com.example.aigc.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ModelConfig {

    private String id;
    private String name;
    private String provider;
    private String modelName;
    private String connectionId;
    private boolean enabled;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public ModelConfig() {
    }

    public static ModelConfig create(
            String name,
            String provider,
            String modelName,
            String connectionId,
            boolean enabled,
            Map<String, Object> metadata
    ) {
        ModelConfig model = new ModelConfig();
        model.id = UUID.randomUUID().toString();
        model.name = name;
        model.provider = provider;
        model.modelName = modelName;
        model.connectionId = connectionId;
        model.enabled = enabled;
        model.metadata = metadata;
        model.createdAt = Instant.now();
        model.updatedAt = model.createdAt;
        return model;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
