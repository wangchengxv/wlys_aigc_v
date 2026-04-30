package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "storyboard_lite_session")
public class StoryboardLiteSession {
    @Id
    @Column(name = "session_id")
    public String sessionId;
    @Column(name = "owner_id")
    public String ownerId;
    @Column(name = "project_id")
    public String projectId;
    public String title;
    public String status;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
