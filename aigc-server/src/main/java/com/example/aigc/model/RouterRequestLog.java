package com.example.aigc.model;

import java.time.Instant;
import java.util.UUID;

public class RouterRequestLog {

    private String id;
    private Instant timestamp;
    private String routerApiKeyId;
    private String connectionId;
    private String connectionName;
    private String provider;
    private String model;
    private String requestFormat;
    private String status;
    private int durationMs;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private String errorMessage;

    public RouterRequestLog() {
    }

    public static RouterRequestLog create() {
        RouterRequestLog log = new RouterRequestLog();
        log.id = UUID.randomUUID().toString();
        log.timestamp = Instant.now();
        return log;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRouterApiKeyId() {
        return routerApiKeyId;
    }

    public void setRouterApiKeyId(String routerApiKeyId) {
        this.routerApiKeyId = routerApiKeyId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRequestFormat() {
        return requestFormat;
    }

    public void setRequestFormat(String requestFormat) {
        this.requestFormat = requestFormat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
