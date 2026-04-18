package com.example.aigc.entity;

import com.example.aigc.enums.DubbingTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "dubbing_task")
public class DubbingTask {
    @Id
    @Column(name = "dubbing_task_id")
    public String dubbingTaskId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "shot_id")
    public String shotId;
    @Column(name = "request_payload_file_id")
    public String requestPayloadFileId;
    @Column(name = "result_audio_file_id")
    public String resultAudioFileId;
    @Column(name = "provider_task_id")
    public String providerTaskId;
    @Column(name = "model_name")
    public String modelName;
    @Column(name = "language")
    public String language;
    @Column(name = "voice_name")
    public String voiceName;
    @Column(name = "speech_rate")
    public Double speechRate;
    @Column(name = "input_text", columnDefinition = "LONGTEXT")
    public String inputText;
    @Enumerated(EnumType.STRING)
    public DubbingTaskStatus status = DubbingTaskStatus.PENDING;
    @Column(name = "retry_count")
    public Integer retryCount = 0;
    @Column(name = "started_at")
    public Instant startedAt;
    @Column(name = "finished_at")
    public Instant finishedAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
}
