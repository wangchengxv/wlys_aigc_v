package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "storyboard_lite_script")
public class StoryboardLiteScript {
    @Id
    @Column(name = "script_id")
    public String scriptId;
    @Column(name = "session_id")
    public String sessionId;
    @Column(name = "script_text", columnDefinition = "LONGTEXT")
    public String scriptText;
    @Column(name = "version_no")
    public int versionNo;
    @Column(name = "created_at")
    public Instant createdAt;
}
