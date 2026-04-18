package com.example.aigc.repository.jpa;

import com.example.aigc.constants.WorkspaceConstants;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.repository.ScriptProjectRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaScriptProjectRepository implements ScriptProjectRepository {
    private final SpringDataScriptProjectRepository projectRepository;
    private final SpringDataContentReviewRecordRepository contentReviewRecordRepository;
    private final SpringDataScriptRevisionRepository revisionRepository;
    private final SpringDataScriptDocumentVersionRepository documentRepository;
    private final SpringDataStoredFileRecordRepository fileRepository;
    private final SpringDataExtractedAssetRepository assetRepository;
    private final SpringDataKeyframeRecordRepository keyframeRepository;
    private final SpringDataStoryboardShotRepository shotRepository;
    private final SpringDataVideoSegmentTaskRepository videoTaskRepository;
    private final SpringDataDubbingTaskRepository dubbingTaskRepository;
    private final SpringDataLipSyncTaskRepository lipSyncTaskRepository;
    private final SpringDataVideoEditDraftRepository videoEditDraftRepository;
    private final SpringDataVideoEditRenderTaskRepository videoEditRenderTaskRepository;
    private final SpringDataFinalCompositionTaskRepository finalCompositionTaskRepository;
    private final SpringDataExportPackageTaskRepository exportPackageTaskRepository;
    private final SpringDataPipelineRunRepository pipelineRunRepository;

    public JpaScriptProjectRepository(
            SpringDataScriptProjectRepository projectRepository,
            SpringDataContentReviewRecordRepository contentReviewRecordRepository,
            SpringDataScriptRevisionRepository revisionRepository,
            SpringDataScriptDocumentVersionRepository documentRepository,
            SpringDataStoredFileRecordRepository fileRepository,
            SpringDataExtractedAssetRepository assetRepository,
            SpringDataKeyframeRecordRepository keyframeRepository,
            SpringDataStoryboardShotRepository shotRepository,
            SpringDataVideoSegmentTaskRepository videoTaskRepository,
            SpringDataDubbingTaskRepository dubbingTaskRepository,
            SpringDataLipSyncTaskRepository lipSyncTaskRepository,
            SpringDataVideoEditDraftRepository videoEditDraftRepository,
            SpringDataVideoEditRenderTaskRepository videoEditRenderTaskRepository,
            SpringDataFinalCompositionTaskRepository finalCompositionTaskRepository,
            SpringDataExportPackageTaskRepository exportPackageTaskRepository,
            SpringDataPipelineRunRepository pipelineRunRepository
    ) {
        this.projectRepository = projectRepository;
        this.contentReviewRecordRepository = contentReviewRecordRepository;
        this.revisionRepository = revisionRepository;
        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
        this.assetRepository = assetRepository;
        this.keyframeRepository = keyframeRepository;
        this.shotRepository = shotRepository;
        this.videoTaskRepository = videoTaskRepository;
        this.dubbingTaskRepository = dubbingTaskRepository;
        this.lipSyncTaskRepository = lipSyncTaskRepository;
        this.videoEditDraftRepository = videoEditDraftRepository;
        this.videoEditRenderTaskRepository = videoEditRenderTaskRepository;
        this.finalCompositionTaskRepository = finalCompositionTaskRepository;
        this.exportPackageTaskRepository = exportPackageTaskRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @Override
    @Transactional
    public ScriptProjectAggregate save(ScriptProjectAggregate aggregate) {
        projectRepository.save(aggregate.project);
        String projectId = aggregate.project.projectId;

        contentReviewRecordRepository.deleteByProjectId(projectId);
        revisionRepository.deleteByProjectId(projectId);
        documentRepository.deleteByProjectId(projectId);
        fileRepository.deleteByProjectId(projectId);
        assetRepository.deleteByProjectId(projectId);
        keyframeRepository.deleteByProjectId(projectId);
        shotRepository.deleteByProjectId(projectId);
        videoTaskRepository.deleteByProjectId(projectId);
        dubbingTaskRepository.deleteByProjectId(projectId);
        lipSyncTaskRepository.deleteByProjectId(projectId);
        videoEditDraftRepository.deleteById(projectId);
        videoEditRenderTaskRepository.deleteByProjectId(projectId);
        finalCompositionTaskRepository.deleteByProjectId(projectId);
        exportPackageTaskRepository.deleteByProjectId(projectId);
        pipelineRunRepository.deleteByProjectId(projectId);

        aggregate.contentReviewRecords.forEach(item -> item.projectId = projectId);
        contentReviewRecordRepository.saveAll(aggregate.contentReviewRecords);
        aggregate.revisions.forEach(item -> item.projectId = projectId);
        revisionRepository.saveAll(aggregate.revisions);
        documentRepository.saveAll(aggregate.documents);
        fileRepository.saveAll(aggregate.files);
        assetRepository.saveAll(aggregate.assets);
        keyframeRepository.saveAll(aggregate.keyframes);
        shotRepository.saveAll(aggregate.shots);
        videoTaskRepository.saveAll(aggregate.videoTasks);
        dubbingTaskRepository.saveAll(aggregate.dubbingTasks);
        lipSyncTaskRepository.saveAll(aggregate.lipSyncTasks);
        if (aggregate.videoEditDraft != null) {
            aggregate.videoEditDraft.projectId = projectId;
            videoEditDraftRepository.save(aggregate.videoEditDraft);
        }
        videoEditRenderTaskRepository.saveAll(aggregate.videoEditRenderTasks);
        finalCompositionTaskRepository.saveAll(aggregate.finalCompositionTasks);
        exportPackageTaskRepository.saveAll(aggregate.exportPackageTasks);
        pipelineRunRepository.saveAll(aggregate.pipelineRuns);
        return aggregate;
    }

    @Override
    public Optional<ScriptProjectAggregate> findById(String projectId) {
        Optional<ScriptProject> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isEmpty()) {
            return Optional.empty();
        }
        ScriptProjectAggregate aggregate = new ScriptProjectAggregate();
        aggregate.project = projectOptional.get();
        aggregate.contentReviewRecords = new ArrayList<>(contentReviewRecordRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId));
        aggregate.revisions = new ArrayList<>(revisionRepository.findAllByProjectId(projectId));
        aggregate.documents = new ArrayList<>(documentRepository.findAllByProjectId(projectId));
        aggregate.files = new ArrayList<>(fileRepository.findAllByProjectId(projectId));
        aggregate.assets = new ArrayList<>(assetRepository.findAllByProjectId(projectId));
        aggregate.keyframes = new ArrayList<>(keyframeRepository.findAllByProjectId(projectId));
        aggregate.shots = new ArrayList<>(shotRepository.findAllByProjectId(projectId));
        aggregate.videoTasks = new ArrayList<>(videoTaskRepository.findAllByProjectId(projectId));
        aggregate.dubbingTasks = new ArrayList<>(dubbingTaskRepository.findAllByProjectId(projectId));
        aggregate.lipSyncTasks = new ArrayList<>(lipSyncTaskRepository.findAllByProjectId(projectId));
        aggregate.videoEditDraft = videoEditDraftRepository.findById(projectId).orElse(null);
        aggregate.videoEditRenderTasks = new ArrayList<>(videoEditRenderTaskRepository.findAllByProjectId(projectId));
        aggregate.finalCompositionTasks = new ArrayList<>(finalCompositionTaskRepository.findAllByProjectId(projectId));
        aggregate.exportPackageTasks = new ArrayList<>(exportPackageTaskRepository.findAllByProjectId(projectId));
        aggregate.pipelineRuns = new ArrayList<>(pipelineRunRepository.findAllByProjectId(projectId));
        return Optional.of(aggregate);
    }

    @Override
    public List<ScriptProjectSummary> findAll(boolean deleted) {
        List<ScriptProject> projects = deleted
                ? projectRepository.findAllByDeletedAtIsNotNull()
                : projectRepository.findAllByDeletedAtIsNull();
        return projects.stream()
                .filter(p -> !WorkspaceConstants.WORKSPACE_PROJECT_ID.equals(p.projectId))
                .map(this::toSummary)
                .sorted(Comparator.comparing((ScriptProjectSummary item) -> item.updatedAt, Comparator.nullsLast(Instant::compareTo)).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void delete(String projectId) {
        projectRepository.findById(projectId).ifPresent(project -> {
            Instant now = Instant.now();
            project.deletedAt = now;
            project.updatedAt = now;
            projectRepository.save(project);
        });
    }

    private ScriptProjectSummary toSummary(ScriptProject project) {
        ScriptProjectSummary summary = new ScriptProjectSummary();
        summary.projectId = project.projectId;
        summary.ownerId = project.ownerId;
        summary.ownerName = project.ownerName;
        summary.orgUnitId = project.orgUnitId;
        summary.courseId = project.courseId;
        summary.name = project.name;
        summary.status = project.status;
        summary.scriptSummary = project.scriptSummary;
        summary.visualStyle = project.visualStyle;
        summary.styleTemplateId = project.styleTemplateId;
        summary.aspectRatio = project.aspectRatio;
        summary.targetDuration = project.targetDuration;
        summary.contentReviewStatus = project.contentReviewStatus;
        summary.reviewResubmitCount = project.reviewResubmitCount;
        summary.latestReviewComment = project.latestReviewComment;
        summary.coverFileId = resolveCoverFileId(project.projectId);
        summary.assetCount = (int) assetRepository.countByProjectId(project.projectId);
        summary.keyframeCount = (int) keyframeRepository.countByProjectId(project.projectId);
        summary.videoTaskCount = (int) videoTaskRepository.countByProjectId(project.projectId);
        summary.createdAt = project.createdAt;
        summary.updatedAt = project.updatedAt;
        summary.deletedAt = project.deletedAt;
        return summary;
    }

    private String resolveCoverFileId(String projectId) {
        for (KeyframeRecord keyframe : keyframeRepository.findAllByProjectId(projectId)) {
            if (keyframe.selected && keyframe.imageFileId != null && !keyframe.imageFileId.isBlank()) {
                return keyframe.imageFileId;
            }
        }
        for (StoredFileRecord file : fileRepository.findAllByProjectId(projectId)) {
            if (file.mediaType != null && file.mediaType.startsWith("image/")) {
                return file.fileId;
            }
        }
        for (StoredFileRecord file : fileRepository.findAllByProjectId(projectId)) {
            if (file.mediaType != null && file.mediaType.startsWith("video/")) {
                return file.fileId;
            }
        }
        return null;
    }
}
