package com.example.aigc.entity;

import com.example.aigc.enums.GenerateMode;
import com.example.aigc.enums.TaskStatus;
import com.example.aigc.repository.jpa.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "generation_task")
public class GenerationTask {
    @Id
    @Column(name = "task_id")
    private String taskId;
    @Column(name = "owner_id")
    private String ownerId;
    @Column(columnDefinition = "LONGTEXT")
    private String prompt;
    @Enumerated(EnumType.STRING)
    private GenerateMode mode;
    private String style;
    @Column(name = "image_model")
    private String imageModel;
    @Column(name = "video_model")
    private String videoModel;
    @Column(name = "image_model_source")
    private String imageModelSource;
    @Column(name = "video_model_source")
    private String videoModelSource;
    @Column(name = "image_model_matched_by")
    private String imageModelMatchedBy;
    @Column(name = "video_model_matched_by")
    private String videoModelMatchedBy;
    @Column(name = "image_model_reject_reason")
    private String imageModelRejectReason;
    @Column(name = "video_model_reject_reason")
    private String videoModelRejectReason;
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    @Column(name = "latency_ms")
    private Long latencyMs;
    @Column(name = "error_code")
    private String errorCode;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "text_results_json", columnDefinition = "LONGTEXT")
    private List<String> textResults;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "image_results_json", columnDefinition = "LONGTEXT")
    private List<String> imageResults;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "video_results_json", columnDefinition = "LONGTEXT")
    private List<String> videoResults;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public GenerateMode getMode() {
        return mode;
    }

    public void setMode(GenerateMode mode) {
        this.mode = mode;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getImageModel() {
        return imageModel;
    }

    public void setImageModel(String imageModel) {
        this.imageModel = imageModel;
    }

    public String getVideoModel() {
        return videoModel;
    }

    public void setVideoModel(String videoModel) {
        this.videoModel = videoModel;
    }

    public String getImageModelSource() {
        return imageModelSource;
    }

    public void setImageModelSource(String imageModelSource) {
        this.imageModelSource = imageModelSource;
    }

    public String getVideoModelSource() {
        return videoModelSource;
    }

    public void setVideoModelSource(String videoModelSource) {
        this.videoModelSource = videoModelSource;
    }

    public String getImageModelMatchedBy() {
        return imageModelMatchedBy;
    }

    public void setImageModelMatchedBy(String imageModelMatchedBy) {
        this.imageModelMatchedBy = imageModelMatchedBy;
    }

    public String getVideoModelMatchedBy() {
        return videoModelMatchedBy;
    }

    public void setVideoModelMatchedBy(String videoModelMatchedBy) {
        this.videoModelMatchedBy = videoModelMatchedBy;
    }

    public String getImageModelRejectReason() {
        return imageModelRejectReason;
    }

    public void setImageModelRejectReason(String imageModelRejectReason) {
        this.imageModelRejectReason = imageModelRejectReason;
    }

    public String getVideoModelRejectReason() {
        return videoModelRejectReason;
    }

    public void setVideoModelRejectReason(String videoModelRejectReason) {
        this.videoModelRejectReason = videoModelRejectReason;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getTextResults() {
        return textResults;
    }

    public void setTextResults(List<String> textResults) {
        this.textResults = textResults;
    }

    public List<String> getImageResults() {
        return imageResults;
    }

    public void setImageResults(List<String> imageResults) {
        this.imageResults = imageResults;
    }

    public List<String> getVideoResults() {
        return videoResults;
    }

    public void setVideoResults(List<String> videoResults) {
        this.videoResults = videoResults;
    }
}
