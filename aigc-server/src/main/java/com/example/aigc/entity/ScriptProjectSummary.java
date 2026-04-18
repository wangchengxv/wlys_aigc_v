package com.example.aigc.entity;

import com.example.aigc.enums.ContentReviewStatus;
import com.example.aigc.enums.ProjectStatus;

import java.time.Instant;

public class ScriptProjectSummary {
    public String projectId;
    public String ownerId;
    public String ownerName;
    public String orgUnitId;
    public String courseId;
    public String name;
    public ProjectStatus status;
    public String scriptSummary;
    public String visualStyle;
    public String styleTemplateId;
    public String aspectRatio;
    public Integer targetDuration;
    public ContentReviewStatus contentReviewStatus;
    public Integer reviewResubmitCount;
    public String latestReviewComment;
    public String coverFileId;
    public int assetCount;
    public int keyframeCount;
    public int videoTaskCount;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant deletedAt;
}
