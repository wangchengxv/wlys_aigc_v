package com.example.aigc.entity;

import com.example.aigc.enums.PipelineStatus;
import com.example.aigc.enums.PipelineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pipeline_run")
public class PipelineRun {
    @Id
    @Column(name = "pipeline_run_id")
    public String pipelineRunId;
    @Column(name = "project_id")
    public String projectId;
    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_type")
    public PipelineType pipelineType;
    @Enumerated(EnumType.STRING)
    public PipelineStatus status;
    @Column(name = "current_stage")
    public String currentStage;
    @Column(name = "total_count")
    public Integer totalCount = 0;
    @Column(name = "success_count")
    public Integer successCount = 0;
    @Column(name = "failed_count")
    public Integer failedCount = 0;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
