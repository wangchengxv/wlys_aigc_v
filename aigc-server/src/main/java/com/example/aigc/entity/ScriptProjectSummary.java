package com.example.aigc.entity;

import com.example.aigc.enums.ProjectStatus;

import java.time.Instant;

public class ScriptProjectSummary {
    public String projectId;
    public String name;
    public ProjectStatus status;
    public String scriptSummary;
    public String visualStyle;
    public String aspectRatio;
    public Integer targetDuration;
    public String coverFileId;
    public int assetCount;
    public int keyframeCount;
    public int videoTaskCount;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant deletedAt;
}
