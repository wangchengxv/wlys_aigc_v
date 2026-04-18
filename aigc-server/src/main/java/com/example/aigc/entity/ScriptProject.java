package com.example.aigc.entity;

import com.example.aigc.enums.ContentReviewStatus;
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
    @Column(name = "owner_id")
    public String ownerId;
    @Column(name = "owner_name")
    public String ownerName;
    @Column(name = "org_unit_id")
    public String orgUnitId;
    @Column(name = "course_id")
    public String courseId;
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
    @Column(name = "style_template_id")
    public String styleTemplateId;
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
    @Column(name = "explicit_tts_model")
    public String explicitTtsModel;
    @Column(name = "dubbing_voice")
    public String dubbingVoice;
    @Column(name = "dubbing_language")
    public String dubbingLanguage;
    @Column(name = "dubbing_speed")
    public Double dubbingSpeed;
    @Column(name = "art_direction_json", columnDefinition = "LONGTEXT")
    public String artDirectionJson;
    /**
     * JSON map: workflow function key -> explicit model name override.
     * Keys are defined in WorkflowModelKey constants.
     * Stored as a single LONGTEXT column to avoid column sprawl.
     */
    @Column(name = "workflow_model_overrides", columnDefinition = "LONGTEXT")
    public String workflowModelOverrides;
    /** JSON 对象：classpath 模板路径 -> 覆盖正文（仅存储与默认不同的项） */
    @Column(name = "prompt_template_overrides", columnDefinition = "LONGTEXT")
    public String promptTemplateOverrides;
    @Enumerated(EnumType.STRING)
    @Column(name = "content_review_status")
    public ContentReviewStatus contentReviewStatus = ContentReviewStatus.NOT_SUBMITTED;
    @Column(name = "current_review_id")
    public String currentReviewId;
    @Column(name = "latest_review_comment", columnDefinition = "LONGTEXT")
    public String latestReviewComment;
    @Column(name = "review_resubmit_count")
    public Integer reviewResubmitCount = 0;
    @Column(name = "review_submitted_at")
    public Instant reviewSubmittedAt;
    @Column(name = "reviewed_at")
    public Instant reviewedAt;
    @Column(name = "reviewer_user_id")
    public String reviewerUserId;
    @Column(name = "reviewer_user_name")
    public String reviewerUserName;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
    @Column(name = "deleted_at")
    public Instant deletedAt;
}
