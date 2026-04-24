package com.example.aigc.model;

import java.util.List;

public class PresetModel {
    private final String id;
    private final String provider;
    private final String modelName;
    private final String baseUrl;
    private final String displayName;
    private final List<String> capabilities;
    private final String endpoint;

    public PresetModel(String provider, String modelName, String baseUrl, String displayName, List<String> capabilities) {
        this(provider, modelName, baseUrl, displayName, capabilities, null);
    }

    public PresetModel(String provider, String modelName, String baseUrl, String displayName, List<String> capabilities, String endpoint) {
        this.id = provider + ":" + modelName + ":" + displayName;
        this.provider = provider;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.displayName = displayName;
        this.capabilities = capabilities;
        this.endpoint = endpoint;
    }

    public String getId() { return id; }
    public String getProvider() { return provider; }
    public String getModelName() { return modelName; }
    public String getBaseUrl() { return baseUrl; }
    public String getDisplayName() { return displayName; }
    public List<String> getCapabilities() { return capabilities; }
    public String getEndpoint() { return endpoint; }
    public boolean hasEndpoint() { return endpoint != null && !endpoint.isEmpty(); }
}
