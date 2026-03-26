package com.example.aigc.entity;

import com.example.aigc.enums.GenerateMode;
import com.example.aigc.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public class GenerationTask {
    private String taskId;
    private String prompt;
    private GenerateMode mode;
    private String style;
    private String imageModel;
    private String videoModel;
    private TaskStatus status;
    private Long latencyMs;
    private String errorCode;
    private LocalDateTime createdAt;
    private List<String> textResults;
    private List<String> imageResults;
    private List<String> videoResults;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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
