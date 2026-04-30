package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "storyboard_lite_video_task")
public class StoryboardLiteVideoTask {
    @Id
    @Column(name = "video_task_id")
    public String videoTaskId;
    @Column(name = "session_id")
    public String sessionId;
    @Column(name = "keyframe_id")
    public String keyframeId;
    @Column(name = "prompt_text", columnDefinition = "TEXT")
    public String promptText;
    @Column(name = "provider_task_id")
    public String providerTaskId;
    public String status;
    @Column(name = "video_url", columnDefinition = "TEXT")
    public String videoUrl;
    @Column(name = "result_video_file_id")
    public String resultVideoFileId;
    @Column(name = "model_name")
    public String modelName;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
