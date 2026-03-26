package com.example.aigc.model;

import java.time.Instant;
import java.util.UUID;

public class ConnectionConfig {

    private String id;
    private String name;
    private String provider;
    private String baseUrl;
    private String encryptedApiKey;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConnectionConfig create(String name, String provider, String baseUrl, String encryptedApiKey, boolean enabled) {
        ConnectionConfig config = new ConnectionConfig();
        config.id = UUID.randomUUID().toString();
        config.name = name;
        config.provider = provider;
        config.baseUrl = baseUrl;
        config.encryptedApiKey = encryptedApiKey;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}