package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "storyboard_lite_keyframe")
public class StoryboardLiteKeyframe {
    @Id
    @Column(name = "keyframe_id")
    public String keyframeId;
    @Column(name = "session_id")
    public String sessionId;
    @Column(name = "prompt_text", columnDefinition = "TEXT")
    public String promptText;
    @Column(name = "image_url", columnDefinition = "TEXT")
    public String imageUrl;
    @Column(name = "image_file_id")
    public String imageFileId;
    @Column(name = "model_name")
    public String modelName;
    public boolean selected;
    public String status;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
