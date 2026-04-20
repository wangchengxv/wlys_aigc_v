package com.example.aigc.repository.jpa;

import com.example.aigc.entity.AuditLogRecord;
import com.example.aigc.entity.AccountImportTask;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.entity.CanvasGraph;
import com.example.aigc.entity.ContentReviewRecord;
import com.example.aigc.entity.DubbingTask;
import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.FinalCompositionTask;
import com.example.aigc.entity.GenerationTask;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.LipSyncTask;
import com.example.aigc.entity.OrgUnit;
import com.example.aigc.entity.PipelineRun;
import com.example.aigc.entity.ReviewRecord;
import com.example.aigc.entity.ScriptDocumentVersion;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptRevision;
import com.example.aigc.entity.SocialAccount;
import com.example.aigc.entity.StyleTemplate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.entity.VideoEditDraft;
import com.example.aigc.entity.VideoEditRenderTask;
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

interface SpringDataVideoEditDraftRepository extends JpaRepository<VideoEditDraft, String> {
}

interface SpringDataStyleTemplateRepository extends JpaRepository<StyleTemplate, String> {
    List<StyleTemplate> findAllByOrderByUpdatedAtDesc();
}

interface SpringDataAuditLogRepository extends JpaRepository<AuditLogRecord, Long> {
    List<AuditLogRecord> findTop200ByOrderByCreatedAtDesc();
}

interface SpringDataAccountImportTaskRepository extends JpaRepository<AccountImportTask, String> {
    List<AccountImportTask> findTop100ByOrderByCreatedAtDesc();
}

interface SpringDataAppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<AppUser> findAllByUserIdIn(List<String> userIds);
}

interface SpringDataSocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<SocialAccount> findAllByUserId(String userId);

    Optional<SocialAccount> findByUserIdAndProvider(String userId, String provider);

    long countByUserId(String userId);
}

interface SpringDataOrgUnitRepository extends JpaRepository<OrgUnit, String> {
}

interface SpringDataTeachingCourseRepository extends JpaRepository<TeachingCourse, String> {
    List<TeachingCourse> findAllByOrderByUpdatedAtDesc();
}

interface SpringDataTeachingAssignmentRepository extends JpaRepository<TeachingAssignment, String> {
    List<TeachingAssignment> findAllByCourseIdOrderByCreatedAtDesc(String courseId);
}

interface SpringDataAssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, String> {
    List<AssignmentSubmission> findAllByAssignmentIdOrderBySubmittedAtDesc(String assignmentId);

    List<AssignmentSubmission> findAllByProjectId(String projectId);
}

interface SpringDataReviewRecordRepository extends JpaRepository<ReviewRecord, String> {
    List<ReviewRecord> findAllBySubmissionIdOrderByCreatedAtDesc(String submissionId);
}

interface SpringDataContentReviewRecordRepository extends JpaRepository<ContentReviewRecord, String> {
    List<ContentReviewRecord> findAllByProjectIdOrderByCreatedAtDesc(String projectId);

    void deleteByProjectId(String projectId);
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

    List<StoredFileRecord> findTop200ByOrderByCreatedAtDesc();

    void deleteByProjectId(String projectId);

    Optional<StoredFileRecord> findByFileId(String fileId);
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

interface SpringDataDubbingTaskRepository extends JpaRepository<DubbingTask, String> {
    List<DubbingTask> findAllByProjectId(String projectId);

    long countByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataLipSyncTaskRepository extends JpaRepository<LipSyncTask, String> {
    List<LipSyncTask> findAllByProjectId(String projectId);

    long countByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataVideoEditRenderTaskRepository extends JpaRepository<VideoEditRenderTask, String> {
    List<VideoEditRenderTask> findAllByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataFinalCompositionTaskRepository extends JpaRepository<FinalCompositionTask, String> {
    List<FinalCompositionTask> findAllByProjectId(String projectId);

    long countByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}

interface SpringDataExportPackageTaskRepository extends JpaRepository<ExportPackageTask, String> {
    List<ExportPackageTask> findAllByProjectId(String projectId);

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
