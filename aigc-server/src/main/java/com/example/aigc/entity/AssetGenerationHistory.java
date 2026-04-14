package com.example.aigc.entity;

import com.example.aigc.enums.AssetHistoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "asset_generation_history")
public class AssetGenerationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, length = 64)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 32)
    private AssetHistoryType assetType;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "file_id", nullable = false, length = 128)
    private String fileId;

    @Column(name = "prompt_text", columnDefinition = "LONGTEXT")
    private String promptText;

    @Column(name = "model_name", length = 255)
    private String modelName;

    @Column(name = "generation_params_json", columnDefinition = "LONGTEXT")
    private String generationParamsJson;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public AssetHistoryType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetHistoryType assetType) {
        this.assetType = assetType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getGenerationParamsJson() {
        return generationParamsJson;
    }

    public void setGenerationParamsJson(String generationParamsJson) {
        this.generationParamsJson = generationParamsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
