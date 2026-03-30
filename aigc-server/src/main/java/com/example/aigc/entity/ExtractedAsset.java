package com.example.aigc.entity;

import com.example.aigc.enums.AssetStatus;
import com.example.aigc.enums.AssetType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtractedAsset {
    public String assetId;
    public String projectId;
    public AssetType assetType;
    public String name;
    public String description;
    public String sourceShotId;
    public List<String> tags = new ArrayList<>();
    public String promptDraft;
    public AssetStatus status = AssetStatus.PENDING;
    public Map<String, Object> metadata = new LinkedHashMap<>();
    public Instant createdAt;
    public Instant updatedAt;
}
