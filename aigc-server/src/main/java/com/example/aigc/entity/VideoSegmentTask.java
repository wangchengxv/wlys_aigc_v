package com.example.aigc.entity;

import com.example.aigc.enums.SegmentTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "video_segment_task")
public class VideoSegmentTask {
    @Id
    @Column(name = "segment_task_id")
    public String segmentTaskId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "shot_id")
    public String shotId;
    @Column(name = "request_payload_file_id")
    public String requestPayloadFileId;
    @Column(name = "result_video_file_id")
    public String resultVideoFileId;
    @Column(name = "provider_task_id")
    public String providerTaskId;
    @Enumerated(EnumType.STRING)
    public SegmentTaskStatus status = SegmentTaskStatus.PENDING;
    @Column(name = "retry_count")
    public Integer retryCount = 0;
    @Column(name = "model_name")
    public String modelName;
    @Column(name = "started_at")
    public Instant startedAt;
    @Column(name = "finished_at")
    public Instant finishedAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
}
