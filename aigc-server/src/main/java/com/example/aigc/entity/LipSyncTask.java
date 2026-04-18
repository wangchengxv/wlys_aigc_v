package com.example.aigc.entity;

import com.example.aigc.enums.LipSyncTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "lip_sync_task")
public class LipSyncTask {
    @Id
    @Column(name = "lip_sync_task_id")
    public String lipSyncTaskId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "shot_id")
    public String shotId;
    @Column(name = "source_video_file_id")
    public String sourceVideoFileId;
    @Column(name = "source_audio_file_id")
    public String sourceAudioFileId;
    @Column(name = "request_payload_file_id")
    public String requestPayloadFileId;
    @Column(name = "result_video_file_id")
    public String resultVideoFileId;
    @Column(name = "provider_task_id")
    public String providerTaskId;
    @Column(name = "model_name")
    public String modelName;
    @Enumerated(EnumType.STRING)
    public LipSyncTaskStatus status = LipSyncTaskStatus.PENDING;
    @Column(name = "retry_count")
    public Integer retryCount = 0;
    @Column(name = "started_at")
    public Instant startedAt;
    @Column(name = "finished_at")
    public Instant finishedAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
}
