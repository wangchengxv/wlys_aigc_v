package com.example.aigc.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionConfig {

    private String id;
    private String name;
    private String provider;
    private String baseUrl;
    private String encryptedApiKey;
    /** Non-secret and encrypted-at-value options (see ConnectionMetadataHelper). */
    private Map<String, Object> metadata;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public ConnectionConfig() {
    }

    public static ConnectionConfig create(String name, String provider, String baseUrl, String encryptedApiKey, boolean enabled) {
        return create(name, provider, baseUrl, encryptedApiKey, enabled, null);
    }

    public static ConnectionConfig create(
            String name,
            String provider,
            String baseUrl,
            String encryptedApiKey,
            boolean enabled,
            Map<String, Object> metadata
    ) {
        ConnectionConfig config = new ConnectionConfig();
        config.id = UUID.randomUUID().toString();
        config.name = name;
        config.provider = provider;
        config.baseUrl = baseUrl;
        config.encryptedApiKey = encryptedApiKey;
        config.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        config.enabled = enabled;
        config.createdAt = Instant.now();
        config.updatedAt = config.createdAt;
        return config;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new HashMap<>() : metadata;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
