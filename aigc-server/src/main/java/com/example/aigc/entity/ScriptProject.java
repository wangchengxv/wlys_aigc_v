package com.example.aigc.entity;

import com.example.aigc.enums.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "script_project")
public class ScriptProject {
    @Id
    @Column(name = "project_id")
    public String projectId;
    public String name;
    @Enumerated(EnumType.STRING)
    public ProjectStatus status = ProjectStatus.DRAFT;
    @Column(name = "source_type")
    public String sourceType;
    @Column(name = "original_script_file_id")
    public String originalScriptFileId;
    @Column(name = "refined_script_file_id")
    public String refinedScriptFileId;
    @Column(name = "refined_script_json_file_id")
    public String refinedScriptJsonFileId;
    @Column(name = "uploaded_source_file_id")
    public String uploadedSourceFileId;
    @Column(name = "script_summary", columnDefinition = "TEXT")
    public String scriptSummary;
    @Column(name = "visual_style", columnDefinition = "LONGTEXT")
    public String visualStyle;
    @Column(name = "aspect_ratio")
    public String aspectRatio;
    @Column(name = "target_duration")
    public Integer targetDuration;
    public String language;
    @Column(name = "explicit_text_model")
    public String explicitTextModel;
    @Column(name = "explicit_image_model")
    public String explicitImageModel;
    @Column(name = "explicit_video_model")
    public String explicitVideoModel;
    @Column(name = "art_direction_json", columnDefinition = "LONGTEXT")
    public String artDirectionJson;
    /**
     * JSON map: workflow function key -> explicit model name override.
     * Keys are defined in WorkflowModelKey constants.
     * Stored as a single LONGTEXT column to avoid column sprawl.
     */
    @Column(name = "workflow_model_overrides", columnDefinition = "LONGTEXT")
    public String workflowModelOverrides;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
    @Column(name = "deleted_at")
    public Instant deletedAt;
}
