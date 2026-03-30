package com.example.aigc.entity;

import com.example.aigc.enums.ProjectStatus;

import java.time.Instant;

public class ScriptProject {
    public String projectId;
    public String name;
    public ProjectStatus status = ProjectStatus.DRAFT;
    public String sourceType;
    public String originalScriptFileId;
    public String refinedScriptFileId;
    public String refinedScriptJsonFileId;
    public String uploadedSourceFileId;
    public String scriptSummary;
    public String visualStyle;
    public String aspectRatio;
    public Integer targetDuration;
    public String language;
    public String explicitTextModel;
    public String explicitImageModel;
    public String explicitVideoModel;
    public Instant createdAt;
    public Instant updatedAt;
}
