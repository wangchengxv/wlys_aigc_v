package com.example.aigc.entity;

import com.example.aigc.enums.FinalCompositionTaskStatus;
import com.example.aigc.repository.jpa.FinalCompositionSegmentListJsonConverter;
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
@Table(name = "final_composition_task")
public class FinalCompositionTask {
    @Id
    @Column(name = "final_composition_task_id")
    public String finalCompositionTaskId;
    @Column(name = "project_id")
    public String projectId;
    @Convert(converter = FinalCompositionSegmentListJsonConverter.class)
    @Column(name = "input_segments_json", columnDefinition = "LONGTEXT")
    public List<FinalCompositionInputSegment> inputSegments = new ArrayList<>();
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
    @Column(name = "started_at")
    public Instant startedAt;
    @Column(name = "finished_at")
    public Instant finishedAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
}
