package com.example.aigc.entity;

import com.example.aigc.enums.PipelineStatus;
import com.example.aigc.enums.PipelineType;

import java.time.Instant;

public class PipelineRun {
    public String pipelineRunId;
    public String projectId;
    public PipelineType pipelineType;
    public PipelineStatus status;
    public String currentStage;
    public Integer totalCount = 0;
    public Integer successCount = 0;
    public Integer failedCount = 0;
    public String errorMessage;
    public Instant createdAt;
    public Instant updatedAt;
}
