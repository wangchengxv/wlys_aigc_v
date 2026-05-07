package com.example.aigc.dto;

import com.example.aigc.entity.PipelineRun;
import com.example.aigc.enums.ProjectStatus;

public class PipelineStatusData {
    public String projectId;
    public ProjectStatus projectStatus;
    public PipelineRun latestRun;
    public int totalCount;
    public int successCount;
    public int failedCount;
    public int runningCount;
    public int queuedCount;
    public int pendingCount;
}
