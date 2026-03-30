package com.example.aigc.model;

import java.time.Instant;
import java.util.UUID;

public class RouterApiKey {

    private String id;
    private String name;
    private String keyValue;
    private boolean active;
    private Instant createdAt;
    private Instant lastUsedAt;

    public RouterApiKey() {
    }

    public static RouterApiKey create(String name, String keyValue) {
        RouterApiKey apiKey = new RouterApiKey();
        apiKey.id = UUID.randomUUID().toString();
        apiKey.name = name;
        apiKey.keyValue = keyValue;
        apiKey.active = true;
        apiKey.createdAt = Instant.now();
        return apiKey;
    }

    public void touchLastUsedAt() {
        this.lastUsedAt = Instant.now();
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

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
