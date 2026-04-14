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
    private final SpringDataScriptRevisionRepository revisionRepository;
    private final SpringDataScriptDocumentVersionRepository documentRepository;
    private final SpringDataStoredFileRecordRepository fileRepository;
    private final SpringDataExtractedAssetRepository assetRepository;
    private final SpringDataKeyframeRecordRepository keyframeRepository;
    private final SpringDataStoryboardShotRepository shotRepository;
    private final SpringDataVideoSegmentTaskRepository videoTaskRepository;
    private final SpringDataPipelineRunRepository pipelineRunRepository;

    public JpaScriptProjectRepository(
            SpringDataScriptProjectRepository projectRepository,
            SpringDataScriptRevisionRepository revisionRepository,
            SpringDataScriptDocumentVersionRepository documentRepository,
            SpringDataStoredFileRecordRepository fileRepository,
            SpringDataExtractedAssetRepository assetRepository,
            SpringDataKeyframeRecordRepository keyframeRepository,
            SpringDataStoryboardShotRepository shotRepository,
            SpringDataVideoSegmentTaskRepository videoTaskRepository,
            SpringDataPipelineRunRepository pipelineRunRepository
    ) {
        this.projectRepository = projectRepository;
        this.revisionRepository = revisionRepository;
        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
        this.assetRepository = assetRepository;
        this.keyframeRepository = keyframeRepository;
        this.shotRepository = shotRepository;
        this.videoTaskRepository = videoTaskRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @Override
    @Transactional
    public ScriptProjectAggregate save(ScriptProjectAggregate aggregate) {
        projectRepository.save(aggregate.project);
        String projectId = aggregate.project.projectId;

        revisionRepository.deleteByProjectId(projectId);
        documentRepository.deleteByProjectId(projectId);
        fileRepository.deleteByProjectId(projectId);
        assetRepository.deleteByProjectId(projectId);
        keyframeRepository.deleteByProjectId(projectId);
        shotRepository.deleteByProjectId(projectId);
        videoTaskRepository.deleteByProjectId(projectId);
        pipelineRunRepository.deleteByProjectId(projectId);

        aggregate.revisions.forEach(item -> item.projectId = projectId);
        revisionRepository.saveAll(aggregate.revisions);
        documentRepository.saveAll(aggregate.documents);
        fileRepository.saveAll(aggregate.files);
        assetRepository.saveAll(aggregate.assets);
        keyframeRepository.saveAll(aggregate.keyframes);
        shotRepository.saveAll(aggregate.shots);
        videoTaskRepository.saveAll(aggregate.videoTasks);
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
        aggregate.revisions = new ArrayList<>(revisionRepository.findAllByProjectId(projectId));
        aggregate.documents = new ArrayList<>(documentRepository.findAllByProjectId(projectId));
        aggregate.files = new ArrayList<>(fileRepository.findAllByProjectId(projectId));
        aggregate.assets = new ArrayList<>(assetRepository.findAllByProjectId(projectId));
        aggregate.keyframes = new ArrayList<>(keyframeRepository.findAllByProjectId(projectId));
        aggregate.shots = new ArrayList<>(shotRepository.findAllByProjectId(projectId));
        aggregate.videoTasks = new ArrayList<>(videoTaskRepository.findAllByProjectId(projectId));
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
        summary.name = project.name;
        summary.status = project.status;
        summary.scriptSummary = project.scriptSummary;
        summary.visualStyle = project.visualStyle;
        summary.aspectRatio = project.aspectRatio;
        summary.targetDuration = project.targetDuration;
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
