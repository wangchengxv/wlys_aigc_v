package com.example.aigc.entity;

import com.example.aigc.enums.AssetStatus;
import com.example.aigc.enums.AssetType;
import com.example.aigc.dto.PromptVersion;
import com.example.aigc.repository.jpa.ObjectMapJsonConverter;
import com.example.aigc.repository.jpa.PromptVersionListJsonConverter;
import com.example.aigc.repository.jpa.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "extracted_asset")
public class ExtractedAsset {
    @Id
    @Column(name = "asset_id")
    public String assetId;
    @Column(name = "project_id")
    public String projectId;
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type")
    public AssetType assetType;
    public String name;
    @Column(columnDefinition = "TEXT")
    public String description;
    @Column(name = "source_shot_id")
    public String sourceShotId;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "tags_json", columnDefinition = "LONGTEXT")
    public List<String> tags = new ArrayList<>();
    @Column(name = "prompt_draft", columnDefinition = "TEXT")
    public String promptDraft;
    @Column(name = "visual_prompt", columnDefinition = "TEXT")
    public String visualPrompt;
    @Convert(converter = PromptVersionListJsonConverter.class)
    @Column(name = "prompt_versions_json", columnDefinition = "LONGTEXT")
    public List<PromptVersion> promptVersions = new ArrayList<>();
    @Column(name = "turnaround_plan_json", columnDefinition = "LONGTEXT")
    public String turnaroundPlanJson;
    @Column(name = "turnaround_image_file_id")
    public String turnaroundImageFileId;
    @Column(name = "storyboard_plan_json", columnDefinition = "LONGTEXT")
    public String storyboardPlanJson;
    @Column(name = "storyboard_translations_json", columnDefinition = "LONGTEXT")
    public String storyboardTranslationsJson;
    @Column(name = "storyboard_prompt_text", columnDefinition = "LONGTEXT")
    public String storyboardPromptText;
    @Column(name = "storyboard_image_file_id")
    public String storyboardImageFileId;
    @Column(name = "three_view_image_file_id")
    public String threeViewImageFileId;
    @Enumerated(EnumType.STRING)
    public AssetStatus status = AssetStatus.PENDING;
    @Convert(converter = ObjectMapJsonConverter.class)
    @Column(name = "metadata_json", columnDefinition = "LONGTEXT")
    public Map<String, Object> metadata = new LinkedHashMap<>();
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
