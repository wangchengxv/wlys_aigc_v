package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "keyframe_record")
public class KeyframeRecord {
    @Id
    @Column(name = "keyframe_id")
    public String keyframeId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "asset_id")
    public String assetId;
    @Column(name = "shot_id")
    public String shotId;
    @Column(name = "prompt_text", columnDefinition = "TEXT")
    public String promptText;
    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    public String negativePrompt;
    @Column(name = "image_file_id")
    public String imageFileId;
    public boolean selected;
    public String status;
    @Column(name = "provider_task_id")
    public String providerTaskId;
    @Column(name = "model_name")
    public String modelName;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
