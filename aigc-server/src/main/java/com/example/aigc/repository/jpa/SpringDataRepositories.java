package com.example.aigc.repository.jpa;

import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.CanvasGraph;
import com.example.aigc.entity.GenerationTask;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.PipelineRun;
import com.example.aigc.entity.ScriptDocumentVersion;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptRevision;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.model.RouterApiKey;
import com.example.aigc.model.RouterRequestLog;
import com.example.aigc.model.RoutingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataConnectionConfigRepository extends JpaRepository<ConnectionConfig, String> {
}

interface SpringDataModelConfigRepository extends JpaRepository<ModelConfig, String> {
}

interface SpringDataGenerationTaskRepository extends JpaRepository<GenerationTask, String> {
    List<GenerationTask> findAllByModeOrderByCreatedAtDesc(com.example.aigc.enums.GenerateMode mode);

    List<GenerationTask> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<GenerationTask> findAllByOwnerIdAndModeOrderByCreatedAtDesc(String ownerId, com.example.aigc.enums.GenerateMode mode);

    long countByOwnerId(String ownerId);

    long countByOwnerIdAndMode(String ownerId, com.example.aigc.enums.GenerateMode mode);

    long countByMode(com.example.aigc.enums.GenerateMode mode);
}

interface SpringDataRouterApiKeyRepository extends JpaRepository<RouterApiKey, String> {
    Optional<RouterApiKey> findByKeyValue(String keyValue);
}

interface SpringDataRoutingConfigRepository extends JpaRepository<RoutingConfig, Long> {
}

interface SpringDataRouterRequestLogRepository extends JpaRepository<RouterRequestLog, String> {
}

interface SpringDataScriptProjectRepository extends JpaRepository<ScriptProject, String> {
    List<ScriptProject> findAllByDeletedAtIsNull();

    List<ScriptProject> findAllByDeletedAtIsNotNull();
}

interface SpringDataScriptRevisionRepository extends JpaRepository<ScriptRevision, String> {
    List<ScriptRevision> findAllByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataScriptDocumentVersionRepository extends JpaRepository<ScriptDocumentVersion, String> {
    List<ScriptDocumentVersion> findAllByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataStoredFileRecordRepository extends JpaRepository<StoredFileRecord, String> {
    List<StoredFileRecord> findAllByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataExtractedAssetRepository extends JpaRepository<ExtractedAsset, String> {
    List<ExtractedAsset> findAllByProjectId(String projectId);

    long countByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataKeyframeRecordRepository extends JpaRepository<KeyframeRecord, String> {
    List<KeyframeRecord> findAllByProjectId(String projectId);

    long countByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataStoryboardShotRepository extends JpaRepository<StoryboardShot, String> {
    List<StoryboardShot> findAllByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataVideoSegmentTaskRepository extends JpaRepository<VideoSegmentTask, String> {
    List<VideoSegmentTask> findAllByProjectId(String projectId);

    long countByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataPipelineRunRepository extends JpaRepository<PipelineRun, String> {
    List<PipelineRun> findAllByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataCanvasGraphRepository extends JpaRepository<CanvasGraph, String> {
    List<CanvasGraph> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    Optional<CanvasGraph> findByIdAndOwnerId(String id, String ownerId);

    void deleteByIdAndOwnerId(String id, String ownerId);
}

