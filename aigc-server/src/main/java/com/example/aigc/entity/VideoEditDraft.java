package com.example.aigc.entity;

import com.example.aigc.repository.jpa.ObjectMapJsonConverter;
import com.example.aigc.repository.jpa.VideoEditSegmentListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "video_edit_draft")
public class VideoEditDraft {
    @Id
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "draft_id")
    public String draftId;
    @Column(name = "version_no")
    public Integer version = 1;
    @Convert(converter = VideoEditSegmentListJsonConverter.class)
    @Column(name = "segments_json", columnDefinition = "LONGTEXT")
    public List<VideoEditSegment> segments = new ArrayList<>();
    @Convert(converter = ObjectMapJsonConverter.class)
    @Column(name = "extensions_json", columnDefinition = "LONGTEXT")
    public Map<String, Object> extensions = new LinkedHashMap<>();
    @Column(name = "published_version")
    public Integer publishedVersion;
    @Column(name = "published_at")
    public Instant publishedAt;
    @Column(name = "published_render_task_id")
    public String publishedRenderTaskId;
    @Column(name = "latest_preview_task_id")
    public String latestPreviewTaskId;
    @Column(name = "latest_publish_task_id")
    public String latestPublishTaskId;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
