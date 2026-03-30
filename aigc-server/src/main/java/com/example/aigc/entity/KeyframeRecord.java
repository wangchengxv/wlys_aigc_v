package com.example.aigc.entity;

import java.time.Instant;

public class KeyframeRecord {
    public String keyframeId;
    public String projectId;
    public String assetId;
    public String shotId;
    public String promptText;
    public String negativePrompt;
    public String imageFileId;
    public boolean selected;
    public String status;
    public String providerTaskId;
    public String modelName;
    public Instant createdAt;
    public Instant updatedAt;
}
