package com.example.aigc.entity;

import com.example.aigc.enums.FinalCompositionTaskStatus;
import com.example.aigc.repository.jpa.VideoEditSegmentListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "video_edit_render_task")
public class VideoEditRenderTask {
    @Id
    @Column(name = "render_task_id")
    public String renderTaskId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "draft_version")
    public Integer draftVersion;
    @Column(name = "task_type")
    public String taskType;
    @Convert(converter = VideoEditSegmentListJsonConverter.class)
    @Column(name = "input_segments_json", columnDefinition = "LONGTEXT")
    public List<VideoEditSegment> inputSegments = new ArrayList<>();
    @Column(name = "request_payload_file_id")
    public String requestPayloadFileId;
    @Column(name = "result_video_file_id")
    public String resultVideoFileId;
    @Column(name = "provider_task_id")
    public String providerTaskId;
    @Column(name = "model_name")
    public String modelName;
    @Enumerated(EnumType.STRING)
    public FinalCompositionTaskStatus status = FinalCompositionTaskStatus.PENDING;
    @Column(name = "retry_count")
    public Integer retryCount = 0;
    @Column(name = "published_at")
    public Instant publishedAt;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "started_at")
    public Instant startedAt;
    @Column(name = "finished_at")
    public Instant finishedAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
}
