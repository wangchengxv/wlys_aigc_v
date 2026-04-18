package com.example.aigc.service;

import com.example.aigc.config.PipelineVideoProperties;
import com.example.aigc.dto.PipelineStatusData;
import com.example.aigc.dto.VideoEditPublishRequest;
import com.example.aigc.entity.DubbingTask;
import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.FinalCompositionInputSegment;
import com.example.aigc.entity.FinalCompositionTask;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.LipSyncTask;
import com.example.aigc.entity.PipelineRun;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoEditDraft;
import com.example.aigc.entity.VideoEditRenderTask;
import com.example.aigc.entity.VideoEditSegment;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.enums.ContentReviewStatus;
import com.example.aigc.enums.AssetHistoryType;
import com.example.aigc.enums.DubbingTaskStatus;
import com.example.aigc.enums.ExportPackageTaskStatus;
import com.example.aigc.enums.FinalCompositionTaskStatus;
import com.example.aigc.enums.LipSyncTaskStatus;
import com.example.aigc.enums.PipelineStatus;
import com.example.aigc.enums.PipelineType;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.SegmentTaskStatus;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.model.WorkflowModelKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ScriptProductionOrchestrator {

    private static final String SAMPLE_VIDEO_URL = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";

    private final ScriptProjectService scriptProjectService;
    private final PromptTemplateService promptTemplateService;
    private final AiCapabilityRoutingService aiCapabilityRoutingService;
    private final ProviderHttpGateway providerHttpGateway;
    private final LocalAssetFileService localAssetFileService;
    private final PipelineVideoProperties pipelineVideoProperties;
    private final Executor videoPipelineExecutor;
    private final VideoStylePresetRegistry videoStylePresetRegistry;
    private final AssetHistoryService assetHistoryService;
    private final VideoEditingService videoEditingService;
    private final ConcurrentHashMap<String, Object> projectLocks = new ConcurrentHashMap<>();

    public ScriptProductionOrchestrator(
            ScriptProjectService scriptProjectService,
            PromptTemplateService promptTemplateService,
            AiCapabilityRoutingService aiCapabilityRoutingService,
            ProviderHttpGateway providerHttpGateway,
            LocalAssetFileService localAssetFileService,
            PipelineVideoProperties pipelineVideoProperties,
            @Qualifier("videoPipelineExecutor") Executor videoPipelineExecutor,
            VideoStylePresetRegistry videoStylePresetRegistry,
            AssetHistoryService assetHistoryService,
            VideoEditingService videoEditingService
    ) {
        this.scriptProjectService = scriptProjectService;
        this.promptTemplateService = promptTemplateService;
        this.aiCapabilityRoutingService = aiCapabilityRoutingService;
        this.providerHttpGateway = providerHttpGateway;
        this.localAssetFileService = localAssetFileService;
        this.pipelineVideoProperties = pipelineVideoProperties;
        this.videoPipelineExecutor = videoPipelineExecutor;
        this.videoStylePresetRegistry = videoStylePresetRegistry;
        this.assetHistoryService = assetHistoryService;
        this.videoEditingService = videoEditingService;
    }

    public PipelineStatusData startVideoGeneration(String projectId) {
        List<String> taskIds;
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            validateBeforeVideoStart(aggregate);
            if (aggregate.shots.isEmpty()) {
                throw new BizException(400, "请先拆分镜头，再启动视频生成");
            }
            if (aggregate.videoTasks.stream().anyMatch(task -> task.status == SegmentTaskStatus.RUNNING || task.status == SegmentTaskStatus.QUEUED)) {
                throw new BizException(400, "当前已有运行中的视频任务，请等待完成后再试");
            }

            aggregate.videoTasks.clear();
            PipelineRun run = new PipelineRun();
            run.pipelineRunId = nextId("run");
            run.projectId = projectId;
            run.pipelineType = PipelineType.VIDEO_GENERATION;
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "VIDEO_GENERATING";
            run.totalCount = aggregate.shots.size();
            run.successCount = 0;
            run.failedCount = 0;
            run.createdAt = Instant.now();
            run.updatedAt = run.createdAt;
            aggregate.pipelineRuns.add(run);
            aggregate.project.status = ProjectStatus.VIDEO_GENERATING;

            taskIds = new ArrayList<>();
            for (StoryboardShot shot : aggregate.shots) {
                VideoSegmentTask task = new VideoSegmentTask();
                task.segmentTaskId = nextId("seg");
                task.projectId = projectId;
                task.shotId = shot.shotId;
                task.status = SegmentTaskStatus.QUEUED;
                task.retryCount = 0;
                aggregate.videoTasks.add(task);
                taskIds.add(task.segmentTaskId);
            }
            scriptProjectService.save(aggregate);
        }

        taskIds.forEach(taskId -> videoPipelineExecutor.execute(() -> processVideoTask(projectId, taskId)));
        return getPipelineStatus(projectId);
    }

    public List<VideoSegmentTask> listVideoTasks(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).videoTasks);
    }

    public PipelineStatusData startDubbingGeneration(String projectId) {
        List<String> taskIds;
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            validateBeforeDubbingStart(aggregate);
            if (aggregate.dubbingTasks.stream().anyMatch(task -> task.status == DubbingTaskStatus.RUNNING || task.status == DubbingTaskStatus.QUEUED)) {
                throw new BizException(400, "当前已有运行中的配音任务，请等待完成后再试");
            }

            aggregate.dubbingTasks.clear();
            taskIds = new ArrayList<>();
            for (StoryboardShot shot : aggregate.shots) {
                DubbingTask task = new DubbingTask();
                task.dubbingTaskId = nextId("dub");
                task.projectId = projectId;
                task.shotId = shot.shotId;
                task.inputText = safe(shot.scriptText).trim();
                task.language = firstNonBlankValue(aggregate.project.dubbingLanguage, aggregate.project.language, "中文");
                task.voiceName = firstNonBlankValue(aggregate.project.dubbingVoice, "通用女声");
                task.speechRate = normalizeSpeechRate(aggregate.project.dubbingSpeed);
                task.status = DubbingTaskStatus.QUEUED;
                task.retryCount = 0;
                aggregate.dubbingTasks.add(task);
                taskIds.add(task.dubbingTaskId);
            }
            aggregate.project.status = ProjectStatus.DUBBING_GENERATING;
            scriptProjectService.save(aggregate);
        }

        taskIds.forEach(taskId -> videoPipelineExecutor.execute(() -> processDubbingTask(projectId, taskId)));
        return getPipelineStatus(projectId);
    }

    public List<DubbingTask> listDubbingTasks(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).dubbingTasks);
    }

    public PipelineStatusData startLipSyncGeneration(String projectId) {
        List<String> taskIds;
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            validateBeforeLipSyncStart(aggregate);
            if (aggregate.lipSyncTasks.stream().anyMatch(task -> task.status == LipSyncTaskStatus.RUNNING || task.status == LipSyncTaskStatus.QUEUED)) {
                throw new BizException(400, "当前已有运行中的口型同步任务，请等待完成后再试");
            }

            aggregate.lipSyncTasks.clear();
            taskIds = new ArrayList<>();
            for (StoryboardShot shot : aggregate.shots) {
                VideoSegmentTask videoTask = findSuccessfulVideoTaskForShot(aggregate, shot.shotId);
                DubbingTask dubbingTask = findSuccessfulDubbingTaskForShot(aggregate, shot.shotId);
                LipSyncTask task = new LipSyncTask();
                task.lipSyncTaskId = nextId("lip");
                task.projectId = projectId;
                task.shotId = shot.shotId;
                task.sourceVideoFileId = videoTask.resultVideoFileId;
                task.sourceAudioFileId = dubbingTask.resultAudioFileId;
                task.status = LipSyncTaskStatus.QUEUED;
                task.retryCount = 0;
                aggregate.lipSyncTasks.add(task);
                taskIds.add(task.lipSyncTaskId);
            }
            aggregate.project.status = ProjectStatus.LIP_SYNC_GENERATING;
            PipelineRun run = new PipelineRun();
            run.pipelineRunId = nextId("run");
            run.projectId = projectId;
            run.pipelineType = PipelineType.LIP_SYNC;
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "LIP_SYNC_GENERATING";
            run.totalCount = aggregate.lipSyncTasks.size();
            run.successCount = 0;
            run.failedCount = 0;
            run.createdAt = Instant.now();
            run.updatedAt = run.createdAt;
            aggregate.pipelineRuns.add(run);
            scriptProjectService.save(aggregate);
        }

        taskIds.forEach(taskId -> videoPipelineExecutor.execute(() -> processLipSyncTask(projectId, taskId)));
        return getPipelineStatus(projectId);
    }

    public List<LipSyncTask> listLipSyncTasks(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).lipSyncTasks);
    }

    public PipelineStatusData startVideoEditPreview(String projectId) {
        return startVideoEditRender(projectId, VideoEditingService.TASK_TYPE_PREVIEW);
    }

    public PipelineStatusData startVideoEditPublish(String projectId, VideoEditPublishRequest request) {
        if (request == null || request.draftVersion == null) {
            throw new BizException(400, "发布剪辑成片必须指定草稿版本");
        }
        if (request != null && request.renderTaskId != null && !request.renderTaskId.isBlank()) {
            return publishVideoEditPreviewResult(projectId, request);
        }
        return startVideoEditRender(
                projectId,
                VideoEditingService.TASK_TYPE_PUBLISH,
                request.draftVersion
        );
    }

    public PipelineStatusData retryVideoEditRenderTask(String projectId, String renderTaskId) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            VideoEditRenderTask task = videoEditingService.findRenderTask(aggregate, renderTaskId);
            if (task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED) {
                throw new BizException(400, "当前剪辑渲染任务正在执行中");
            }
            int nextRetry = task.retryCount == null ? 1 : task.retryCount + 1;
            if (nextRetry > Math.max(1, pipelineVideoProperties.getMaxRetries())) {
                throw new BizException(400, "该任务已达到最大重试次数");
            }
            VideoEditDraft draft = videoEditingService.requireRenderableDraft(aggregate);
            task.retryCount = nextRetry;
            task.status = FinalCompositionTaskStatus.QUEUED;
            task.errorMessage = null;
            task.draftVersion = draft.version;
            task.inputSegments = copyVideoEditSegments(draft.segments);
            task.requestPayloadFileId = null;
            task.resultVideoFileId = null;
            task.providerTaskId = null;
            task.modelName = null;
            task.publishedAt = null;
            task.startedAt = null;
            task.finishedAt = null;
            PipelineRun run = latestRunByType(aggregate, pipelineTypeForVideoEditTask(task.taskType));
            if (run == null) {
                run = new PipelineRun();
                run.pipelineRunId = nextId("run");
                run.projectId = projectId;
                run.pipelineType = pipelineTypeForVideoEditTask(task.taskType);
                run.totalCount = 1;
                run.createdAt = Instant.now();
                aggregate.pipelineRuns.add(run);
            }
            run.status = PipelineStatus.RUNNING;
            run.currentStage = currentStageForVideoEditTask(task.taskType);
            run.updatedAt = Instant.now();
            scriptProjectService.save(aggregate);
        }
        videoPipelineExecutor.execute(() -> processVideoEditRenderTask(projectId, renderTaskId));
        return getPipelineStatus(projectId);
    }

    private PipelineStatusData startVideoEditRender(String projectId, String taskType) {
        return startVideoEditRender(projectId, taskType, null);
    }

    private PipelineStatusData startVideoEditRender(String projectId, String taskType, Integer expectedDraftVersion) {
        String taskId;
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            if (aggregate.videoEditRenderTasks.stream().anyMatch(task ->
                    Objects.equals(normalizeVideoEditTaskType(task.taskType), normalizeVideoEditTaskType(taskType))
                            && (task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED))) {
                throw new BizException(400, "当前已有运行中的剪辑" +
                        (VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(taskType) ? "发布" : "预览") + "任务，请等待完成后再试");
            }
            VideoEditDraft draft = videoEditingService.requireRenderableDraft(aggregate);
            if (expectedDraftVersion != null && !Objects.equals(expectedDraftVersion, draft.version)) {
                throw new BizException(409, "剪辑草稿版本已变更，请刷新后重试");
            }
            VideoEditRenderTask task = new VideoEditRenderTask();
            task.renderTaskId = nextId("vedit");
            task.projectId = projectId;
            task.draftVersion = draft.version;
            task.taskType = normalizeVideoEditTaskType(taskType);
            task.inputSegments = copyVideoEditSegments(draft.segments);
            task.status = FinalCompositionTaskStatus.QUEUED;
            task.retryCount = 0;
            task.createdAt = Instant.now();
            aggregate.videoEditRenderTasks.add(task);
            if (VideoEditingService.TASK_TYPE_PREVIEW.equalsIgnoreCase(taskType)) {
                draft.latestPreviewTaskId = task.renderTaskId;
            } else {
                draft.latestPublishTaskId = task.renderTaskId;
            }
            draft.updatedAt = Instant.now();

            PipelineRun run = new PipelineRun();
            run.pipelineRunId = nextId("run");
            run.projectId = projectId;
            run.pipelineType = pipelineTypeForVideoEditTask(taskType);
            run.status = PipelineStatus.RUNNING;
            run.currentStage = currentStageForVideoEditTask(taskType);
            run.totalCount = 1;
            run.successCount = 0;
            run.failedCount = 0;
            run.createdAt = Instant.now();
            run.updatedAt = run.createdAt;
            aggregate.pipelineRuns.add(run);
            scriptProjectService.save(aggregate);
            taskId = task.renderTaskId;
        }

        videoPipelineExecutor.execute(() -> processVideoEditRenderTask(projectId, taskId));
        return getPipelineStatus(projectId);
    }

    private PipelineStatusData publishVideoEditPreviewResult(String projectId, VideoEditPublishRequest request) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            VideoEditDraft draft = videoEditingService.requireRenderableDraft(aggregate);
            VideoEditRenderTask task = videoEditingService.findRenderTask(aggregate, request.renderTaskId);
            validatePublishPreviewRequest(aggregate, task, request.draftVersion);

            Instant publishedAt = Instant.now();
            task.publishedAt = publishedAt;
            draft.publishedVersion = task.draftVersion;
            draft.publishedAt = publishedAt;
            draft.publishedRenderTaskId = task.renderTaskId;
            draft.latestPublishTaskId = task.renderTaskId;
            draft.updatedAt = publishedAt;

            PipelineRun run = new PipelineRun();
            run.pipelineRunId = nextId("run");
            run.projectId = projectId;
            run.pipelineType = PipelineType.VIDEO_EDIT_PUBLISH;
            run.status = PipelineStatus.SUCCESS;
            run.currentStage = currentStageForVideoEditTask(VideoEditingService.TASK_TYPE_PUBLISH);
            run.totalCount = 1;
            run.successCount = 1;
            run.failedCount = 0;
            run.createdAt = publishedAt;
            run.updatedAt = publishedAt;
            aggregate.pipelineRuns.add(run);

            scriptProjectService.save(aggregate);
        }
        return getPipelineStatus(projectId);
    }

    public PipelineStatusData startFinalComposition(String projectId) {
        String taskId;
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            validateBeforeFinalCompositionStart(aggregate);
            if (aggregate.finalCompositionTasks.stream().anyMatch(task -> task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED)) {
                throw new BizException(400, "当前已有运行中的成片任务，请等待完成后再试");
            }

            FinalCompositionTask task = new FinalCompositionTask();
            task.finalCompositionTaskId = nextId("final");
            task.projectId = projectId;
            task.inputSegments = collectCompositionInputSegments(aggregate);
            task.status = FinalCompositionTaskStatus.QUEUED;
            task.retryCount = 0;
            aggregate.finalCompositionTasks.clear();
            aggregate.finalCompositionTasks.add(task);
            aggregate.project.status = ProjectStatus.FINAL_COMPOSITION_GENERATING;

            PipelineRun run = new PipelineRun();
            run.pipelineRunId = nextId("run");
            run.projectId = projectId;
            run.pipelineType = PipelineType.FINAL_COMPOSITION;
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "FINAL_COMPOSITION_GENERATING";
            run.totalCount = aggregate.finalCompositionTasks.size();
            run.successCount = 0;
            run.failedCount = 0;
            run.createdAt = Instant.now();
            run.updatedAt = run.createdAt;
            aggregate.pipelineRuns.add(run);
            scriptProjectService.save(aggregate);
            taskId = task.finalCompositionTaskId;
        }

        videoPipelineExecutor.execute(() -> processFinalCompositionTask(projectId, taskId));
        return getPipelineStatus(projectId);
    }

    public List<FinalCompositionTask> listFinalCompositionTasks(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).finalCompositionTasks);
    }

    public PipelineStatusData startExportPackage(String projectId) {
        String taskId;
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            validateBeforeExportPackageStart(aggregate);
            if (aggregate.exportPackageTasks.stream().anyMatch(task -> task.status == ExportPackageTaskStatus.RUNNING || task.status == ExportPackageTaskStatus.QUEUED)) {
                throw new BizException(400, "当前已有运行中的导出包任务，请等待完成后再试");
            }

            ExportSource source = requireExportSource(aggregate);
            ExportPackageTask task = new ExportPackageTask();
            task.exportPackageTaskId = nextId("pkg");
            task.projectId = projectId;
            applyExportSource(task, source);
            task.status = ExportPackageTaskStatus.QUEUED;
            task.retryCount = 0;
            aggregate.exportPackageTasks.clear();
            aggregate.exportPackageTasks.add(task);
            aggregate.project.status = ProjectStatus.EXPORT_PACKAGE_GENERATING;

            PipelineRun run = new PipelineRun();
            run.pipelineRunId = nextId("run");
            run.projectId = projectId;
            run.pipelineType = PipelineType.EXPORT_PACKAGE;
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "EXPORT_PACKAGE_GENERATING";
            run.totalCount = aggregate.exportPackageTasks.size();
            run.successCount = 0;
            run.failedCount = 0;
            run.createdAt = Instant.now();
            run.updatedAt = run.createdAt;
            aggregate.pipelineRuns.add(run);
            scriptProjectService.save(aggregate);
            taskId = task.exportPackageTaskId;
        }

        videoPipelineExecutor.execute(() -> processExportPackageTask(projectId, taskId));
        return getPipelineStatus(projectId);
    }

    public List<ExportPackageTask> listExportPackageTasks(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).exportPackageTasks);
    }

    public PipelineStatusData retryVideoTask(String projectId, String segmentTaskId) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            VideoSegmentTask task = findTask(aggregate, segmentTaskId);
            if (task.status == SegmentTaskStatus.RUNNING || task.status == SegmentTaskStatus.QUEUED) {
                throw new BizException(400, "当前视频任务正在执行中");
            }
            int nextRetry = task.retryCount == null ? 1 : task.retryCount + 1;
            if (nextRetry > Math.max(1, pipelineVideoProperties.getMaxRetries())) {
                throw new BizException(400, "该任务已达到最大重试次数");
            }
            if (task.resultVideoFileId != null && !task.resultVideoFileId.isBlank()) {
                assetHistoryService.appendSnapshot(
                        projectId,
                        AssetHistoryType.VIDEO,
                        task.segmentTaskId,
                        task.resultVideoFileId,
                        null,
                        task.modelName,
                        null
                );
            }
            task.retryCount = nextRetry;
            task.status = SegmentTaskStatus.QUEUED;
            task.errorMessage = null;
            task.resultVideoFileId = null;
            task.providerTaskId = null;
            task.requestPayloadFileId = null;
            task.startedAt = null;
            task.finishedAt = null;
            aggregate.project.status = ProjectStatus.VIDEO_GENERATING;

            PipelineRun run = latestRunByType(aggregate, PipelineType.VIDEO_GENERATION);
            if (run == null) {
                run = new PipelineRun();
                run.pipelineRunId = nextId("run");
                run.projectId = projectId;
                run.pipelineType = PipelineType.VIDEO_GENERATION;
                run.totalCount = aggregate.videoTasks.size();
                run.createdAt = Instant.now();
                aggregate.pipelineRuns.add(run);
            }
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "VIDEO_GENERATING";
            run.updatedAt = Instant.now();
            scriptProjectService.save(aggregate);
        }
        videoPipelineExecutor.execute(() -> processVideoTask(projectId, segmentTaskId));
        return getPipelineStatus(projectId);
    }

    public PipelineStatusData retryDubbingTask(String projectId, String dubbingTaskId) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            DubbingTask task = findDubbingTask(aggregate, dubbingTaskId);
            if (task.status == DubbingTaskStatus.RUNNING || task.status == DubbingTaskStatus.QUEUED) {
                throw new BizException(400, "当前配音任务正在执行中");
            }
            int nextRetry = task.retryCount == null ? 1 : task.retryCount + 1;
            if (nextRetry > Math.max(1, pipelineVideoProperties.getMaxRetries())) {
                throw new BizException(400, "该任务已达到最大重试次数");
            }
            task.retryCount = nextRetry;
            task.status = DubbingTaskStatus.QUEUED;
            task.errorMessage = null;
            task.resultAudioFileId = null;
            task.providerTaskId = null;
            task.requestPayloadFileId = null;
            task.startedAt = null;
            task.finishedAt = null;
            task.speechRate = normalizeSpeechRate(task.speechRate);
            aggregate.project.status = ProjectStatus.DUBBING_GENERATING;
            scriptProjectService.save(aggregate);
        }
        videoPipelineExecutor.execute(() -> processDubbingTask(projectId, dubbingTaskId));
        return getPipelineStatus(projectId);
    }

    public PipelineStatusData retryLipSyncTask(String projectId, String lipSyncTaskId) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            LipSyncTask task = findLipSyncTask(aggregate, lipSyncTaskId);
            if (task.status == LipSyncTaskStatus.RUNNING || task.status == LipSyncTaskStatus.QUEUED) {
                throw new BizException(400, "当前口型同步任务正在执行中");
            }
            int nextRetry = task.retryCount == null ? 1 : task.retryCount + 1;
            if (nextRetry > Math.max(1, pipelineVideoProperties.getMaxRetries())) {
                throw new BizException(400, "该任务已达到最大重试次数");
            }
            VideoSegmentTask videoTask = findSuccessfulVideoTaskForShot(aggregate, task.shotId);
            DubbingTask dubbingTask = findSuccessfulDubbingTaskForShot(aggregate, task.shotId);
            if (task.resultVideoFileId != null && !task.resultVideoFileId.isBlank()) {
                assetHistoryService.appendSnapshot(
                        projectId,
                        AssetHistoryType.LIP_SYNC_VIDEO,
                        task.lipSyncTaskId,
                        task.resultVideoFileId,
                        null,
                        task.modelName,
                        null
                );
            }
            task.retryCount = nextRetry;
            task.status = LipSyncTaskStatus.QUEUED;
            task.errorMessage = null;
            task.sourceVideoFileId = videoTask.resultVideoFileId;
            task.sourceAudioFileId = dubbingTask.resultAudioFileId;
            task.resultVideoFileId = null;
            task.providerTaskId = null;
            task.requestPayloadFileId = null;
            task.startedAt = null;
            task.finishedAt = null;
            aggregate.project.status = ProjectStatus.LIP_SYNC_GENERATING;

            PipelineRun run = latestRunByType(aggregate, PipelineType.LIP_SYNC);
            if (run == null) {
                run = new PipelineRun();
                run.pipelineRunId = nextId("run");
                run.projectId = projectId;
                run.pipelineType = PipelineType.LIP_SYNC;
                run.totalCount = aggregate.lipSyncTasks.size();
                run.createdAt = Instant.now();
                aggregate.pipelineRuns.add(run);
            }
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "LIP_SYNC_GENERATING";
            run.updatedAt = Instant.now();
            scriptProjectService.save(aggregate);
        }
        videoPipelineExecutor.execute(() -> processLipSyncTask(projectId, lipSyncTaskId));
        return getPipelineStatus(projectId);
    }

    public PipelineStatusData retryFinalCompositionTask(String projectId, String finalCompositionTaskId) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            FinalCompositionTask task = findFinalCompositionTask(aggregate, finalCompositionTaskId);
            if (task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED) {
                throw new BizException(400, "当前成片任务正在执行中");
            }
            int nextRetry = task.retryCount == null ? 1 : task.retryCount + 1;
            if (nextRetry > Math.max(1, pipelineVideoProperties.getMaxRetries())) {
                throw new BizException(400, "该任务已达到最大重试次数");
            }
            task.retryCount = nextRetry;
            task.status = FinalCompositionTaskStatus.QUEUED;
            task.errorMessage = null;
            task.inputSegments = collectCompositionInputSegments(aggregate);
            task.resultVideoFileId = null;
            task.providerTaskId = null;
            task.requestPayloadFileId = null;
            task.startedAt = null;
            task.finishedAt = null;
            aggregate.project.status = ProjectStatus.FINAL_COMPOSITION_GENERATING;

            PipelineRun run = latestRunByType(aggregate, PipelineType.FINAL_COMPOSITION);
            if (run == null) {
                run = new PipelineRun();
                run.pipelineRunId = nextId("run");
                run.projectId = projectId;
                run.pipelineType = PipelineType.FINAL_COMPOSITION;
                run.totalCount = aggregate.finalCompositionTasks.size();
                run.createdAt = Instant.now();
                aggregate.pipelineRuns.add(run);
            }
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "FINAL_COMPOSITION_GENERATING";
            run.updatedAt = Instant.now();
            scriptProjectService.save(aggregate);
        }
        videoPipelineExecutor.execute(() -> processFinalCompositionTask(projectId, finalCompositionTaskId));
        return getPipelineStatus(projectId);
    }

    public PipelineStatusData retryExportPackageTask(String projectId, String exportPackageTaskId) {
        synchronized (lock(projectId)) {
            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            ExportPackageTask task = findExportPackageTask(aggregate, exportPackageTaskId);
            if (task.status == ExportPackageTaskStatus.RUNNING || task.status == ExportPackageTaskStatus.QUEUED) {
                throw new BizException(400, "当前导出包任务正在执行中");
            }
            int nextRetry = task.retryCount == null ? 1 : task.retryCount + 1;
            if (nextRetry > Math.max(1, pipelineVideoProperties.getMaxRetries())) {
                throw new BizException(400, "该任务已达到最大重试次数");
            }
            ExportSource source = requireExportSource(aggregate);
            task.retryCount = nextRetry;
            task.status = ExportPackageTaskStatus.QUEUED;
            task.errorMessage = null;
            applyExportSource(task, source);
            task.manifestFileId = null;
            task.resultArchiveFileId = null;
            task.archiveStorageProvider = null;
            task.archiveBucketName = null;
            task.archiveObjectKey = null;
            task.archivePublicUrl = null;
            task.manifestStorageProvider = null;
            task.manifestBucketName = null;
            task.manifestObjectKey = null;
            task.manifestPublicUrl = null;
            task.startedAt = null;
            task.finishedAt = null;
            aggregate.project.status = ProjectStatus.EXPORT_PACKAGE_GENERATING;

            PipelineRun run = latestRunByType(aggregate, PipelineType.EXPORT_PACKAGE);
            if (run == null) {
                run = new PipelineRun();
                run.pipelineRunId = nextId("run");
                run.projectId = projectId;
                run.pipelineType = PipelineType.EXPORT_PACKAGE;
                run.totalCount = aggregate.exportPackageTasks.size();
                run.createdAt = Instant.now();
                aggregate.pipelineRuns.add(run);
            }
            run.status = PipelineStatus.RUNNING;
            run.currentStage = "EXPORT_PACKAGE_GENERATING";
            run.updatedAt = Instant.now();
            scriptProjectService.save(aggregate);
        }
        videoPipelineExecutor.execute(() -> processExportPackageTask(projectId, exportPackageTaskId));
        return getPipelineStatus(projectId);
    }

    public PipelineStatusData getPipelineStatus(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        PipelineStatusData data = new PipelineStatusData();
        data.projectId = projectId;
        data.projectStatus = resolveProjectStatus(aggregate);
        data.latestRun = latestProductionRun(aggregate);
        data.totalCount = aggregate.videoTasks.size();
        data.successCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.SUCCESS).count();
        data.failedCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.FAILED).count();
        data.runningCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.RUNNING).count();
        data.queuedCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.QUEUED).count();
        data.pendingCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.PENDING).count();
        data.videoTaskCount = aggregate.videoTasks.size();
        data.dubbingTaskCount = aggregate.dubbingTasks.size();
        data.dubbingSuccessCount = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.SUCCESS).count();
        data.dubbingFailedCount = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.FAILED).count();
        data.dubbingRunningCount = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.RUNNING).count();
        data.dubbingQueuedCount = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.QUEUED).count();
        data.dubbingPendingCount = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.PENDING).count();
        data.lipSyncTaskCount = aggregate.lipSyncTasks.size();
        data.lipSyncSuccessCount = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.SUCCESS).count();
        data.lipSyncFailedCount = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.FAILED).count();
        data.lipSyncRunningCount = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.RUNNING).count();
        data.lipSyncQueuedCount = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.QUEUED).count();
        data.lipSyncPendingCount = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.PENDING).count();
        data.videoEditRenderTaskCount = aggregate.videoEditRenderTasks.size();
        data.videoEditPreviewTaskCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> VideoEditingService.TASK_TYPE_PREVIEW.equalsIgnoreCase(safe(task.taskType)))
                .count();
        data.videoEditPublishTaskCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(safe(task.taskType)))
                .count();
        data.videoEditRenderSuccessCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.SUCCESS)
                .count();
        data.videoEditRenderFailedCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.FAILED)
                .count();
        data.videoEditRenderRunningCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.RUNNING)
                .count();
        data.videoEditRenderQueuedCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.QUEUED)
                .count();
        data.videoEditRenderPendingCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.PENDING)
                .count();
        VideoEditDraft videoEditDraft = videoEditingService.getDraftOrNull(aggregate);
        if (videoEditDraft != null) {
            data.videoEditDraftVersion = videoEditDraft.version;
            data.videoEditPublishedVersion = videoEditDraft.publishedVersion;
            data.videoEditHasPublishedResult = videoEditingService.toResponse(aggregate, videoEditDraft).hasPublishedResult;
            data.videoEditHasUnpublishedChanges = videoEditingService.toResponse(aggregate, videoEditDraft).hasUnpublishedChanges;
        }
        data.finalCompositionTaskCount = aggregate.finalCompositionTasks.size();
        data.finalCompositionSuccessCount = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.SUCCESS).count();
        data.finalCompositionFailedCount = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.FAILED).count();
        data.finalCompositionRunningCount = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.RUNNING).count();
        data.finalCompositionQueuedCount = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.QUEUED).count();
        data.finalCompositionPendingCount = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.PENDING).count();
        data.exportPackageTaskCount = aggregate.exportPackageTasks.size();
        data.exportPackageSuccessCount = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.SUCCESS).count();
        data.exportPackageFailedCount = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.FAILED).count();
        data.exportPackageRunningCount = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.RUNNING).count();
        data.exportPackageQueuedCount = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.QUEUED).count();
        data.exportPackagePendingCount = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.PENDING).count();
        data.contentReviewStatus = aggregate.project.contentReviewStatus == null
                ? ContentReviewStatus.NOT_SUBMITTED
                : aggregate.project.contentReviewStatus;
        data.reviewResubmitCount = aggregate.project.reviewResubmitCount == null ? 0 : aggregate.project.reviewResubmitCount;
        data.currentReviewId = aggregate.project.currentReviewId;
        data.latestReviewComment = aggregate.project.latestReviewComment;
        data.videoReady = !aggregate.videoTasks.isEmpty() && data.successCount == aggregate.videoTasks.size();
        data.dubbingReady = !aggregate.dubbingTasks.isEmpty() && data.dubbingSuccessCount == aggregate.dubbingTasks.size();
        data.lipSyncReady = !aggregate.lipSyncTasks.isEmpty() && data.lipSyncSuccessCount == aggregate.lipSyncTasks.size();
        data.finalCompositionReady = !aggregate.finalCompositionTasks.isEmpty()
                && data.finalCompositionSuccessCount == aggregate.finalCompositionTasks.size();
        data.exportPackageReady = !aggregate.exportPackageTasks.isEmpty()
                && data.exportPackageSuccessCount == aggregate.exportPackageTasks.size();
        return data;
    }

    private void processVideoTask(String projectId, String segmentTaskId) {
        try {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                VideoSegmentTask task = findTask(aggregate, segmentTaskId);
                task.status = SegmentTaskStatus.RUNNING;
                task.startedAt = Instant.now();
                task.errorMessage = null;
                scriptProjectService.save(aggregate);
            }

            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            VideoSegmentTask currentTask = findTask(aggregate, segmentTaskId);
            StoryboardShot shot = findShot(aggregate, currentTask.shotId);
            Map<String, Object> requestPayload = buildVideoRequestPayload(aggregate, shot);
            StoredFileRecord requestFile = localAssetFileService.storeJson(projectId, "video/" + shot.shotId + "/request.json", requestPayload);

            String effectiveVideoModel = scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.VIDEO_GENERATION, "video");
            AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveVideo(effectiveVideoModel);
            StoredFileRecord resultVideo;
            String providerTaskId = null;
            if (resolvedModel.hasProvider() && resolvedModel.apiKey() != null && !resolvedModel.apiKey().isBlank()) {
                VideoCallResult callResult = invokeVideoModel(resolvedModel, requestPayload);
                providerTaskId = callResult.providerTaskId;
                resultVideo = localAssetFileService.storeRemote(projectId, "video/" + shot.shotId + "/result.mp4", "video/mp4", callResult.videoUrl);
            } else {
                resultVideo = localAssetFileService.storeRemote(projectId, "video/" + shot.shotId + "/result.mp4", "video/mp4", SAMPLE_VIDEO_URL);
            }

            synchronized (lock(projectId)) {
                ScriptProjectAggregate latest = scriptProjectService.require(projectId);
                VideoSegmentTask task = findTask(latest, segmentTaskId);
                if (task.resultVideoFileId != null && !task.resultVideoFileId.isBlank()) {
                    assetHistoryService.appendSnapshot(
                            projectId,
                            AssetHistoryType.VIDEO,
                            task.segmentTaskId,
                            task.resultVideoFileId,
                            null,
                            task.modelName,
                            null
                    );
                }
                scriptProjectService.upsertFile(latest, requestFile);
                scriptProjectService.upsertFile(latest, resultVideo);
                task.requestPayloadFileId = requestFile.fileId;
                task.resultVideoFileId = resultVideo.fileId;
                task.providerTaskId = providerTaskId;
                task.modelName = resolvedModel.modelName();
                task.status = SegmentTaskStatus.SUCCESS;
                task.finishedAt = Instant.now();
                refreshVideoRun(latest);
                scriptProjectService.save(latest);
            }
        } catch (Exception ex) {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                VideoSegmentTask task = findTask(aggregate, segmentTaskId);
                task.status = SegmentTaskStatus.FAILED;
                task.finishedAt = Instant.now();
                task.errorMessage = ex instanceof BizException ? ex.getMessage() : "视频生成失败";
                refreshVideoRun(aggregate);
                scriptProjectService.save(aggregate);
            }
        }
    }

    private void processDubbingTask(String projectId, String dubbingTaskId) {
        try {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                DubbingTask task = findDubbingTask(aggregate, dubbingTaskId);
                task.status = DubbingTaskStatus.RUNNING;
                task.startedAt = Instant.now();
                task.errorMessage = null;
                scriptProjectService.save(aggregate);
            }

            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            DubbingTask currentTask = findDubbingTask(aggregate, dubbingTaskId);
            StoryboardShot shot = findShot(aggregate, currentTask.shotId);
            Map<String, Object> requestPayload = buildDubbingRequestPayload(aggregate, shot, currentTask);
            StoredFileRecord requestFile = localAssetFileService.storeJson(projectId, "audio/" + shot.shotId + "/request.json", requestPayload);

            String effectiveTtsModel = scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.TTS_DUBBING, "tts");
            AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveTts(effectiveTtsModel);
            if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
                String reason = effectiveTtsModel == null || effectiveTtsModel.isBlank()
                        ? "未配置可用的配音模型，请先在模型配置中启用支持 tts 能力的模型"
                        : "未命中已配置的配音模型：" + effectiveTtsModel;
                throw new BizException(400, reason);
            }
            byte[] audioBytes = synthesizeDubbingAudioBytes(currentTask.inputText, currentTask.voiceName, currentTask.speechRate);
            StoredFileRecord resultAudio = localAssetFileService.storeBytes(projectId, "audio/" + shot.shotId + "/result.wav", "audio/wav", audioBytes);

            synchronized (lock(projectId)) {
                ScriptProjectAggregate latest = scriptProjectService.require(projectId);
                DubbingTask task = findDubbingTask(latest, dubbingTaskId);
                scriptProjectService.upsertFile(latest, requestFile);
                scriptProjectService.upsertFile(latest, resultAudio);
                task.requestPayloadFileId = requestFile.fileId;
                task.resultAudioFileId = resultAudio.fileId;
                task.providerTaskId = "mock-tts-" + shot.shotId;
                task.modelName = resolvedModel.modelName();
                task.status = DubbingTaskStatus.SUCCESS;
                task.finishedAt = Instant.now();
                latest.project.status = resolveProjectStatus(latest);
                scriptProjectService.save(latest);
            }
        } catch (Exception ex) {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                DubbingTask task = findDubbingTask(aggregate, dubbingTaskId);
                task.status = DubbingTaskStatus.FAILED;
                task.finishedAt = Instant.now();
                task.errorMessage = ex instanceof BizException ? ex.getMessage() : "配音生成失败";
                aggregate.project.status = resolveProjectStatus(aggregate);
                scriptProjectService.save(aggregate);
            }
        }
    }

    private void processLipSyncTask(String projectId, String lipSyncTaskId) {
        try {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                LipSyncTask task = findLipSyncTask(aggregate, lipSyncTaskId);
                task.status = LipSyncTaskStatus.RUNNING;
                task.startedAt = Instant.now();
                task.errorMessage = null;
                scriptProjectService.save(aggregate);
            }

            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            LipSyncTask currentTask = findLipSyncTask(aggregate, lipSyncTaskId);
            StoryboardShot shot = findShot(aggregate, currentTask.shotId);
            StoredFileRecord sourceVideo = requireStoredFile(aggregate, currentTask.sourceVideoFileId, "源视频");
            StoredFileRecord sourceAudio = requireStoredFile(aggregate, currentTask.sourceAudioFileId, "配音音频");
            Map<String, Object> requestPayload = buildLipSyncRequestPayload(aggregate, shot, currentTask, sourceVideo, sourceAudio);
            StoredFileRecord requestFile = localAssetFileService.storeJson(projectId, "lip-sync/" + shot.shotId + "/request.json", requestPayload);
            StoredFileRecord resultVideo = localAssetFileService.storeRemote(
                    projectId,
                    "lip-sync/" + shot.shotId + "/result.mp4",
                    "video/mp4",
                    SAMPLE_VIDEO_URL
            );
            String modelName = firstNonBlank(
                    scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.LIP_SYNC, "video"),
                    "mock-lip-sync"
            );

            synchronized (lock(projectId)) {
                ScriptProjectAggregate latest = scriptProjectService.require(projectId);
                LipSyncTask task = findLipSyncTask(latest, lipSyncTaskId);
                if (task.resultVideoFileId != null && !task.resultVideoFileId.isBlank()) {
                    assetHistoryService.appendSnapshot(
                            projectId,
                            AssetHistoryType.LIP_SYNC_VIDEO,
                            task.lipSyncTaskId,
                            task.resultVideoFileId,
                            null,
                            task.modelName,
                            null
                    );
                }
                scriptProjectService.upsertFile(latest, requestFile);
                scriptProjectService.upsertFile(latest, resultVideo);
                task.requestPayloadFileId = requestFile.fileId;
                task.resultVideoFileId = resultVideo.fileId;
                task.providerTaskId = "mock-lip-sync-" + shot.shotId;
                task.modelName = modelName;
                task.status = LipSyncTaskStatus.SUCCESS;
                task.finishedAt = Instant.now();
                refreshLipSyncRun(latest);
                scriptProjectService.save(latest);
            }
        } catch (Exception ex) {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                LipSyncTask task = findLipSyncTask(aggregate, lipSyncTaskId);
                task.status = LipSyncTaskStatus.FAILED;
                task.finishedAt = Instant.now();
                task.errorMessage = ex instanceof BizException ? ex.getMessage() : "口型同步失败";
                refreshLipSyncRun(aggregate);
                scriptProjectService.save(aggregate);
            }
        }
    }

    private void processVideoEditRenderTask(String projectId, String renderTaskId) {
        try {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                VideoEditRenderTask task = videoEditingService.findRenderTask(aggregate, renderTaskId);
                task.status = FinalCompositionTaskStatus.RUNNING;
                task.startedAt = Instant.now();
                task.errorMessage = null;
                scriptProjectService.save(aggregate);
            }

            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            VideoEditRenderTask currentTask = videoEditingService.findRenderTask(aggregate, renderTaskId);
            if (currentTask.inputSegments == null || currentTask.inputSegments.isEmpty()) {
                throw new BizException(400, "剪辑渲染任务缺少可用片段，请重新保存草稿后再试");
            }
            for (VideoEditSegment segment : currentTask.inputSegments) {
                requireStoredFile(aggregate, segment.sourceFileId, "剪辑片段源视频");
            }
            Map<String, Object> requestPayload = buildVideoEditRenderRequestPayload(aggregate, currentTask);
            String folder = VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(currentTask.taskType) ? "publish" : "preview";
            StoredFileRecord requestFile = localAssetFileService.storeJson(
                    projectId,
                    "video-editing/" + folder + "/" + renderTaskId + "/request.json",
                    requestPayload
            );
            StoredFileRecord resultVideo = localAssetFileService.storeRemote(
                    projectId,
                    "video-editing/" + folder + "/" + renderTaskId + "/result.mp4",
                    "video/mp4",
                    SAMPLE_VIDEO_URL
            );
            String modelName = firstNonBlank(
                    scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.VIDEO_GENERATION, "video"),
                    "mock-video-edit-render"
            );

            synchronized (lock(projectId)) {
                ScriptProjectAggregate latest = scriptProjectService.require(projectId);
                VideoEditRenderTask task = videoEditingService.findRenderTask(latest, renderTaskId);
                scriptProjectService.upsertFile(latest, requestFile);
                scriptProjectService.upsertFile(latest, resultVideo);
                task.requestPayloadFileId = requestFile.fileId;
                task.resultVideoFileId = resultVideo.fileId;
                task.providerTaskId = "mock-video-edit-" + renderTaskId;
                task.modelName = modelName;
                task.status = FinalCompositionTaskStatus.SUCCESS;
                task.finishedAt = Instant.now();
                if (VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(task.taskType)) {
                    task.publishedAt = task.finishedAt;
                    VideoEditDraft draft = videoEditingService.getDraftOrNull(latest);
                    if (draft != null) {
                        draft.publishedVersion = task.draftVersion;
                        draft.publishedAt = task.finishedAt;
                        draft.publishedRenderTaskId = task.renderTaskId;
                        draft.latestPublishTaskId = task.renderTaskId;
                        draft.updatedAt = task.finishedAt;
                    }
                } else {
                    VideoEditDraft draft = videoEditingService.getDraftOrNull(latest);
                    if (draft != null) {
                        draft.latestPreviewTaskId = task.renderTaskId;
                        draft.updatedAt = task.finishedAt;
                    }
                }
                refreshVideoEditRun(latest, task.taskType);
                scriptProjectService.save(latest);
            }
        } catch (Exception ex) {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                VideoEditRenderTask task = videoEditingService.findRenderTask(aggregate, renderTaskId);
                task.status = FinalCompositionTaskStatus.FAILED;
                task.finishedAt = Instant.now();
                task.errorMessage = ex instanceof BizException ? ex.getMessage() : "剪辑渲染失败";
                refreshVideoEditRun(aggregate, task.taskType);
                scriptProjectService.save(aggregate);
            }
        }
    }

    private void validatePublishPreviewRequest(
            ScriptProjectAggregate aggregate,
            VideoEditRenderTask task,
            Integer expectedDraftVersion
    ) {
        if (expectedDraftVersion == null) {
            throw new BizException(400, "发布指定预览结果时必须提供草稿版本");
        }
        if (!VideoEditingService.TASK_TYPE_PREVIEW.equalsIgnoreCase(task.taskType)) {
            throw new BizException(400, "仅支持发布成功的剪辑预览结果");
        }
        if (task.status != FinalCompositionTaskStatus.SUCCESS) {
            throw new BizException(400, "仅支持发布成功的剪辑预览结果");
        }
        if (expectedDraftVersion != null && !Objects.equals(expectedDraftVersion, task.draftVersion)) {
            throw new BizException(400, "指定草稿版本与预览结果不匹配");
        }
        if (task.resultVideoFileId == null || task.resultVideoFileId.isBlank() || !hasAvailableStoredFile(aggregate, task.resultVideoFileId)) {
            throw new BizException(400, "指定预览结果文件不存在，请重新生成预览后再试");
        }
    }

    private void processFinalCompositionTask(String projectId, String finalCompositionTaskId) {
        try {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                FinalCompositionTask task = findFinalCompositionTask(aggregate, finalCompositionTaskId);
                task.status = FinalCompositionTaskStatus.RUNNING;
                task.startedAt = Instant.now();
                task.errorMessage = null;
                scriptProjectService.save(aggregate);
            }

            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            FinalCompositionTask currentTask = findFinalCompositionTask(aggregate, finalCompositionTaskId);
            List<FinalCompositionInputSegment> inputSegments = currentTask.inputSegments == null
                    ? List.of()
                    : new ArrayList<>(currentTask.inputSegments);
            if (inputSegments.isEmpty()) {
                throw new BizException(400, "成片任务缺少可用输入片段，请重新创建任务");
            }
            for (FinalCompositionInputSegment segment : inputSegments) {
                requireStoredFile(aggregate, segment.sourceFileId, "成片输入视频");
            }
            Map<String, Object> requestPayload = buildFinalCompositionRequestPayload(aggregate, currentTask, inputSegments);
            StoredFileRecord requestFile = localAssetFileService.storeJson(
                    projectId,
                    "final-composition/" + finalCompositionTaskId + "/request.json",
                    requestPayload
            );
            StoredFileRecord resultVideo = localAssetFileService.storeRemote(
                    projectId,
                    "final-composition/" + finalCompositionTaskId + "/result.mp4",
                    "video/mp4",
                    SAMPLE_VIDEO_URL
            );
            String modelName = firstNonBlank(
                    scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.VIDEO_GENERATION, "video"),
                    "mock-final-composition"
            );

            synchronized (lock(projectId)) {
                ScriptProjectAggregate latest = scriptProjectService.require(projectId);
                FinalCompositionTask task = findFinalCompositionTask(latest, finalCompositionTaskId);
                scriptProjectService.upsertFile(latest, requestFile);
                scriptProjectService.upsertFile(latest, resultVideo);
                task.requestPayloadFileId = requestFile.fileId;
                task.resultVideoFileId = resultVideo.fileId;
                task.providerTaskId = "mock-final-composition-" + latest.project.projectId;
                task.modelName = modelName;
                task.status = FinalCompositionTaskStatus.SUCCESS;
                task.finishedAt = Instant.now();
                refreshFinalCompositionRun(latest);
                scriptProjectService.save(latest);
            }
        } catch (Exception ex) {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                FinalCompositionTask task = findFinalCompositionTask(aggregate, finalCompositionTaskId);
                task.status = FinalCompositionTaskStatus.FAILED;
                task.finishedAt = Instant.now();
                task.errorMessage = ex instanceof BizException ? ex.getMessage() : "成片编排失败";
                refreshFinalCompositionRun(aggregate);
                scriptProjectService.save(aggregate);
            }
        }
    }

    private void processExportPackageTask(String projectId, String exportPackageTaskId) {
        try {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                ExportPackageTask task = findExportPackageTask(aggregate, exportPackageTaskId);
                task.status = ExportPackageTaskStatus.RUNNING;
                task.startedAt = Instant.now();
                task.errorMessage = null;
                scriptProjectService.save(aggregate);
            }

            ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
            ExportPackageTask currentTask = findExportPackageTask(aggregate, exportPackageTaskId);
            ExportSource source = requireExportSource(aggregate);
            StoredFileRecord finalVideo = requireStoredFile(aggregate, source.videoFileId(), "项目级成片");
            Map<String, Object> manifest = buildExportPackageManifest(aggregate, currentTask, source, finalVideo);
            StoredFileRecord manifestFile = localAssetFileService.storeJson(
                    projectId,
                    "export-package/" + exportPackageTaskId + "/manifest.json",
                    manifest
            );
            byte[] archiveBytes = buildExportPackageArchive(
                    manifestFile,
                    finalVideo,
                    safeFileName(aggregate.project.name, "script-project") + "-final.mp4"
            );
            StoredFileRecord archiveFile = localAssetFileService.storeBytes(
                    projectId,
                    "export-package/" + exportPackageTaskId + "/package.zip",
                    "application/zip",
                    archiveBytes
            );

            synchronized (lock(projectId)) {
                ScriptProjectAggregate latest = scriptProjectService.require(projectId);
                ExportPackageTask task = findExportPackageTask(latest, exportPackageTaskId);
                scriptProjectService.upsertFile(latest, manifestFile);
                scriptProjectService.upsertFile(latest, archiveFile);
                applyExportSource(task, source);
                task.sourceFinalVideoFileId = finalVideo.fileId;
                task.manifestFileId = manifestFile.fileId;
                task.resultArchiveFileId = archiveFile.fileId;
                applyStoredFileMetadata(task, manifestFile, archiveFile);
                task.status = ExportPackageTaskStatus.SUCCESS;
                task.finishedAt = Instant.now();
                refreshExportPackageRun(latest);
                scriptProjectService.save(latest);
            }
        } catch (Exception ex) {
            synchronized (lock(projectId)) {
                ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
                ExportPackageTask task = findExportPackageTask(aggregate, exportPackageTaskId);
                task.status = ExportPackageTaskStatus.FAILED;
                task.finishedAt = Instant.now();
                task.errorMessage = ex instanceof BizException ? ex.getMessage() : "导出包生成失败";
                refreshExportPackageRun(aggregate);
                scriptProjectService.save(aggregate);
            }
        }
    }

    private Map<String, Object> buildVideoRequestPayload(ScriptProjectAggregate aggregate, StoryboardShot shot) {
        int defaultTargetDurationSec = aggregate.project.targetDuration == null ? 15 : aggregate.project.targetDuration;
        int effectiveTargetDurationSec = shot.targetDurationSec == null || shot.targetDurationSec <= 0
                ? defaultTargetDurationSec
                : shot.targetDurationSec;

        List<ExtractedAsset> relatedAssets = aggregate.assets.stream()
                .filter(asset -> containsAny(shot.characterRefs, asset.assetId)
                        || containsAny(shot.backgroundRefs, asset.assetId)
                        || containsAny(shot.propRefs, asset.assetId))
                .toList();
        List<KeyframeRecord> relatedKeyframes = aggregate.keyframes.stream()
                .filter(item -> item.selected && containsAny(shot.keyframeRefs, item.keyframeId))
                .toList();
        String firstFrameFileId = resolveFirstFrameFileId(shot);
        String firstFrameUrl = firstFrameFileId == null ? "" : localAssetFileService.toPublicUrl(firstFrameFileId);
        String firstFrameGuidance = firstFrameFileId == null
                ? "未指定额外首帧参考图"
                : ("首帧模式=" + safe(shot.firstFrameMode) + "，参考图=" + firstFrameUrl);

        Map<String, Object> promptVars = new LinkedHashMap<>();
        promptVars.put("projectName", safe(aggregate.project.name));
        promptVars.put("visualStyle", safe(videoStylePresetRegistry.resolveAnchorForRead(aggregate.project.visualStyle)));
        promptVars.put("aspectRatio", safe(aggregate.project.aspectRatio));
        promptVars.put("targetDuration", String.valueOf(effectiveTargetDurationSec));
        promptVars.put("shotTitle", safe(shot.title));
        promptVars.put("shotScript", safe(shot.scriptText));
        promptVars.put("actionSummary", safe(shot.actionSummary));
        promptVars.put("cameraMovement", safe(shot.cameraMovement));
        promptVars.put("assetSummary", relatedAssets.stream().map(asset -> asset.name + "：" + asset.description).reduce((left, right) -> left + "\n" + right).orElse("无"));
        promptVars.put("keyframeSummary", relatedKeyframes.stream().map(item -> item.promptText).reduce((left, right) -> left + "\n" + right).orElse("无"));
        promptVars.put("firstFrameGuidance", firstFrameGuidance);
        String prompt = promptTemplateService.renderForProject(
                aggregate.project,
                "prompts/storyboard/build-video-segment.md",
                promptVars,
                """
                        请基于以下镜头信息生成适合视频模型的提示词：
                        镜头标题：{{shotTitle}}
                        镜头脚本：{{shotScript}}
                        动作摘要：{{actionSummary}}
                        运镜：{{cameraMovement}}
                        资产：{{assetSummary}}
                        关键帧：{{keyframeSummary}}
                        首帧参考：{{firstFrameGuidance}}
                        风格：{{visualStyle}}
                        比例：{{aspectRatio}}
                        时长：{{targetDuration}} 秒
                        """
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        String buildPayloadVideoModel = scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.VIDEO_GENERATION, "video");
        payload.put("model", aiCapabilityRoutingService.resolveVideo(buildPayloadVideoModel).modelName());
        payload.put("prompt", prompt);
        payload.put("aspectRatio", aggregate.project.aspectRatio);
        payload.put("targetDuration", effectiveTargetDurationSec);
        payload.put("shot", shot);
        payload.put("assets", relatedAssets);
        payload.put("keyframes", relatedKeyframes);
        payload.put("firstFrameImageFileId", firstFrameFileId);
        payload.put("firstFrameImageUrl", firstFrameUrl);
        Map<String, Object> firstFrameAudit = new LinkedHashMap<>();
        firstFrameAudit.put("mode", safe(shot.firstFrameMode));
        firstFrameAudit.put("storyboardAssetId", safe(shot.storyboardAssetId));
        firstFrameAudit.put("storyboardImageFileId", safe(shot.storyboardImageFileId));
        firstFrameAudit.put("storyboardCropFileId", safe(shot.storyboardCropFileId));
        firstFrameAudit.put("storyboardCropIndex", shot.storyboardCropIndex);
        firstFrameAudit.put("resolvedFirstFrameFileId", firstFrameFileId);
        firstFrameAudit.put("resolvedFirstFrameImageUrl", firstFrameUrl);
        firstFrameAudit.put("boundAt", shot.updatedAt == null ? null : shot.updatedAt.toString());
        payload.put("firstFrameAudit", firstFrameAudit);
        return payload;
    }

    private String resolveFirstFrameFileId(StoryboardShot shot) {
        if (shot == null) {
            return null;
        }
        if ("CROPPED_PANEL".equalsIgnoreCase(safe(shot.firstFrameMode))) {
            return shot.storyboardCropFileId;
        }
        if ("FULL_GRID".equalsIgnoreCase(safe(shot.firstFrameMode))) {
            return shot.storyboardImageFileId;
        }
        return null;
    }

    private VideoCallResult invokeVideoModel(
            AiCapabilityRoutingService.ResolvedAiModel resolvedModel,
            Map<String, Object> requestPayload
    ) {
        Map<String, Object> providerPayload = new LinkedHashMap<>();
        if (resolvedModel.provider().gatewayKind() == GatewayKind.MOARK_I2V) {
            String imageUrl = String.valueOf(requestPayload.getOrDefault("firstFrameImageUrl", "")).trim();
            if (imageUrl.isBlank()) {
                throw new BizException(400, "Moark 图生视频需要首帧参考图，请在分镜中设置首帧");
            }
            providerPayload.put("prompt", String.valueOf(requestPayload.get("prompt")));
            providerPayload.put("model", resolvedModel.modelName());
            providerPayload.put("num_inference_steps", 50);
            providerPayload.put("num_frames", 81);
            providerPayload.put("image", imageUrl);
        } else {
            providerPayload.put("model", resolvedModel.modelName());
            providerPayload.put("content", List.of(Map.of("type", "text", "text", String.valueOf(requestPayload.get("prompt")))));
        }
        try {
            Map<String, Object> submitBody = providerHttpGateway.submitVideoTask(
                    resolvedModel.provider(),
                    resolvedModel.connection() == null ? resolvedModel.provider().defaultBaseUrl() : resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    providerPayload,
                    Duration.ofSeconds(60)
            );
            String directUrl = parseVideoUrl(submitBody);
            if (directUrl != null) {
                return new VideoCallResult(directUrl, parseTaskId(submitBody));
            }
            String taskId = parseTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "视频模型未返回任务标识");
            }
            String videoUrl = pollVideoResult(resolvedModel, taskId);
            return new VideoCallResult(videoUrl, taskId);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            if (resolvedModel.systemFallback()) {
                return new VideoCallResult(SAMPLE_VIDEO_URL, null);
            }
            throw new BizException(502, "视频模型调用失败");
        }
    }

    private String pollVideoResult(AiCapabilityRoutingService.ResolvedAiModel resolvedModel, String taskId) {
        int maxAttempts = Math.max(1, 40);
        long intervalMs = Math.max(500L, pipelineVideoProperties.getPollIntervalMs());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<String, Object> result = providerHttpGateway.queryVideoTask(
                        resolvedModel.provider(),
                        resolvedModel.connection() == null ? resolvedModel.provider().defaultBaseUrl() : resolvedModel.connection().getBaseUrl(),
                        resolvedModel.apiKey(),
                        taskId,
                        Duration.ofSeconds(30)
                );
                Object errNode = result.get("error");
                if (errNode != null && !Boolean.FALSE.equals(errNode) && !"false".equalsIgnoreCase(String.valueOf(errNode).trim())) {
                    throw new BizException(502, parseTaskError(result));
                }
                String url = parseVideoUrl(result);
                if (url != null) {
                    return url;
                }
                String status = parseTaskStatus(result);
                if (isFailedStatus(status)) {
                    throw new BizException(502, "视频任务失败：" + safe(parseTaskError(result)));
                }
                Thread.sleep(intervalMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new BizException(504, "视频任务轮询被中断");
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BizException(502, "视频任务查询失败");
            }
        }
        throw new BizException(504, "视频生成超时，请稍后重试");
    }

    private Map<String, Object> buildDubbingRequestPayload(ScriptProjectAggregate aggregate, StoryboardShot shot, DubbingTask task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", aggregate.project.projectId);
        payload.put("shotId", shot.shotId);
        payload.put("shotTitle", shot.title);
        payload.put("text", task.inputText);
        payload.put("language", task.language);
        payload.put("voiceName", task.voiceName);
        payload.put("speechRate", normalizeSpeechRate(task.speechRate));
        payload.put("model", scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.TTS_DUBBING, "tts"));
        payload.put("extensibleStages", List.of("lip_sync", "mixing", "package_export"));
        return payload;
    }

    private Map<String, Object> buildLipSyncRequestPayload(
            ScriptProjectAggregate aggregate,
            StoryboardShot shot,
            LipSyncTask task,
            StoredFileRecord sourceVideo,
            StoredFileRecord sourceAudio
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", aggregate.project.projectId);
        payload.put("shotId", shot.shotId);
        payload.put("shotTitle", shot.title);
        payload.put("sourceVideoFileId", sourceVideo.fileId);
        payload.put("sourceVideoUrl", localAssetFileService.toPublicUrl(sourceVideo.fileId));
        payload.put("sourceAudioFileId", sourceAudio.fileId);
        payload.put("sourceAudioUrl", localAssetFileService.toPublicUrl(sourceAudio.fileId));
        payload.put("model", firstNonBlank(
                scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.LIP_SYNC, "video"),
                "mock-lip-sync"
        ));
        payload.put("extensibleStages", List.of("timeline_edit", "bgm_mixing", "package_export"));
        payload.put("resultPathHint", "lip-sync/" + shot.shotId + "/result.mp4");
        payload.put("taskId", task.lipSyncTaskId);
        return payload;
    }

    private Map<String, Object> buildVideoEditRenderRequestPayload(
            ScriptProjectAggregate aggregate,
            VideoEditRenderTask task
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", aggregate.project.projectId);
        payload.put("projectName", aggregate.project.name);
        payload.put("taskId", task.renderTaskId);
        payload.put("taskType", normalizeVideoEditTaskType(task.taskType));
        payload.put("draftVersion", task.draftVersion);
        payload.put("aspectRatio", aggregate.project.aspectRatio);
        payload.put("targetDuration", aggregate.project.targetDuration);
        payload.put("inputSegments", task.inputSegments == null ? List.of() : task.inputSegments);
        payload.put("model", firstNonBlank(
                scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.VIDEO_GENERATION, "video"),
                "mock-video-edit-render"
        ));
        payload.put("extensibleStages", List.of("multi_track_audio", "subtitle_burn", "transition_pack"));
        payload.put("resultPathHint", "video-editing/" +
                (VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(task.taskType) ? "publish" : "preview") +
                "/" + task.renderTaskId + "/result.mp4");
        return payload;
    }

    private Map<String, Object> buildFinalCompositionRequestPayload(
            ScriptProjectAggregate aggregate,
            FinalCompositionTask task,
            List<FinalCompositionInputSegment> inputSegments
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", aggregate.project.projectId);
        payload.put("projectName", aggregate.project.name);
        payload.put("aspectRatio", aggregate.project.aspectRatio);
        payload.put("targetDuration", aggregate.project.targetDuration);
        payload.put("taskId", task.finalCompositionTaskId);
        payload.put("inputSegments", inputSegments);
        payload.put("model", firstNonBlank(
                scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.VIDEO_GENERATION, "video"),
                "mock-final-composition"
        ));
        payload.put("extensibleStages", List.of("multi_track_mixing", "subtitle_burn", "package_export"));
        payload.put("resultPathHint", "final-composition/" + task.finalCompositionTaskId + "/result.mp4");
        return payload;
    }

    private void validateBeforeDubbingStart(ScriptProjectAggregate aggregate) {
        if (aggregate.shots == null || aggregate.shots.isEmpty()) {
            throw new BizException(400, "请先拆分镜头，再启动配音生成");
        }
        List<String> missing = aggregate.shots.stream()
                .filter(Objects::nonNull)
                .filter(shot -> safe(shot.scriptText).isBlank())
                .map(shot -> safe(shot.title).isBlank() ? shot.shotId : shot.title)
                .toList();
        if (!missing.isEmpty()) {
            throw new BizException(400, "以下镜头缺少配音文本：" + String.join("、", missing));
        }
    }

    private void validateBeforeLipSyncStart(ScriptProjectAggregate aggregate) {
        if (aggregate.shots == null || aggregate.shots.isEmpty()) {
            throw new BizException(400, "请先拆分镜头，再启动口型同步");
        }
        if (aggregate.videoTasks.stream().anyMatch(task -> task.status == SegmentTaskStatus.RUNNING || task.status == SegmentTaskStatus.QUEUED)) {
            throw new BizException(400, "请等待视频任务完成后再启动口型同步");
        }
        if (aggregate.dubbingTasks.stream().anyMatch(task -> task.status == DubbingTaskStatus.RUNNING || task.status == DubbingTaskStatus.QUEUED)) {
            throw new BizException(400, "请等待配音任务完成后再启动口型同步");
        }
        List<String> missingVideo = new ArrayList<>();
        List<String> missingAudio = new ArrayList<>();
        for (StoryboardShot shot : aggregate.shots) {
            if (findSuccessfulVideoTaskForShot(aggregate, shot.shotId, false) == null) {
                missingVideo.add(shotDisplayName(shot));
            }
            if (findSuccessfulDubbingTaskForShot(aggregate, shot.shotId, false) == null) {
                missingAudio.add(shotDisplayName(shot));
            }
        }
        if (!missingVideo.isEmpty() || !missingAudio.isEmpty()) {
            List<String> errors = new ArrayList<>();
            if (!missingVideo.isEmpty()) {
                errors.add("以下镜头缺少源视频：" + String.join("、", missingVideo));
            }
            if (!missingAudio.isEmpty()) {
                errors.add("以下镜头缺少配音音频：" + String.join("、", missingAudio));
            }
            throw new BizException(400, String.join("；", errors));
        }
    }

    private void validateBeforeFinalCompositionStart(ScriptProjectAggregate aggregate) {
        if (aggregate.shots == null || aggregate.shots.isEmpty()) {
            throw new BizException(400, "请先拆分镜头，再启动成片编排");
        }
        if (aggregate.videoTasks.stream().anyMatch(task -> task.status == SegmentTaskStatus.RUNNING || task.status == SegmentTaskStatus.QUEUED)) {
            throw new BizException(400, "请等待视频任务完成后再启动成片编排");
        }
        if (aggregate.lipSyncTasks.stream().anyMatch(task -> task.status == LipSyncTaskStatus.RUNNING || task.status == LipSyncTaskStatus.QUEUED)) {
            throw new BizException(400, "请等待口型同步任务完成后再启动成片编排");
        }
        collectCompositionInputSegments(aggregate);
    }

    private void validateBeforeExportPackageStart(ScriptProjectAggregate aggregate) {
        if (aggregate.finalCompositionTasks.stream().anyMatch(task -> task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED)) {
            throw new BizException(400, "请等待项目级成片生成完成后再创建导出包");
        }
        requireExportSource(aggregate);
    }

    private void validateBeforeVideoStart(ScriptProjectAggregate aggregate) {
        if (aggregate.assets.isEmpty()) {
            throw new BizException(400, "请先抽取资产并生成关键帧");
        }
        Set<String> confirmedAssetIds = aggregate.keyframes.stream()
                .filter(item -> item.selected)
                .map(item -> item.assetId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> requiredAssetIds = new LinkedHashSet<>();
        for (StoryboardShot shot : aggregate.shots) {
            if (shot.characterRefs != null) {
                requiredAssetIds.addAll(shot.characterRefs);
            }
            if (shot.backgroundRefs != null) {
                requiredAssetIds.addAll(shot.backgroundRefs);
            }
            if (shot.propRefs != null) {
                requiredAssetIds.addAll(shot.propRefs);
            }
        }
        if (requiredAssetIds.isEmpty()) {
            return;
        }
        List<String> missing = aggregate.assets.stream()
                .filter(asset -> requiredAssetIds.contains(asset.assetId))
                .filter(asset -> !confirmedAssetIds.contains(asset.assetId))
                .map(asset -> asset.name)
                .toList();
        if (!missing.isEmpty()) {
            throw new BizException(400, "以下资产尚未确认关键帧：" + String.join("、", missing));
        }
    }

    private void refreshVideoRun(ScriptProjectAggregate aggregate) {
        PipelineRun run = latestRunByType(aggregate, PipelineType.VIDEO_GENERATION);
        if (run == null) {
            return;
        }
        int success = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.SUCCESS).count();
        int failed = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.FAILED).count();
        int active = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.RUNNING || task.status == SegmentTaskStatus.QUEUED).count();
        run.totalCount = aggregate.videoTasks.size();
        run.successCount = success;
        run.failedCount = failed;
        run.updatedAt = Instant.now();
        if (active > 0) {
            run.status = PipelineStatus.RUNNING;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed == 0 && success == aggregate.videoTasks.size()) {
            run.status = PipelineStatus.SUCCESS;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (success > 0 && failed > 0) {
            run.status = PipelineStatus.PARTIAL_FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed > 0) {
            run.status = PipelineStatus.FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
        }
    }

    private void refreshLipSyncRun(ScriptProjectAggregate aggregate) {
        PipelineRun run = latestRunByType(aggregate, PipelineType.LIP_SYNC);
        if (run == null) {
            return;
        }
        int success = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.SUCCESS).count();
        int failed = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.FAILED).count();
        int active = (int) aggregate.lipSyncTasks.stream()
                .filter(task -> task.status == LipSyncTaskStatus.RUNNING || task.status == LipSyncTaskStatus.QUEUED)
                .count();
        run.totalCount = aggregate.lipSyncTasks.size();
        run.successCount = success;
        run.failedCount = failed;
        run.updatedAt = Instant.now();
        if (active > 0) {
            run.status = PipelineStatus.RUNNING;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed == 0 && success == aggregate.lipSyncTasks.size()) {
            run.status = PipelineStatus.SUCCESS;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (success > 0 && failed > 0) {
            run.status = PipelineStatus.PARTIAL_FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed > 0) {
            run.status = PipelineStatus.FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
        }
    }

    private void refreshVideoEditRun(ScriptProjectAggregate aggregate, String taskType) {
        PipelineRun run = latestRunByType(aggregate, pipelineTypeForVideoEditTask(taskType));
        if (run == null) {
            return;
        }
        int success = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> Objects.equals(normalizeVideoEditTaskType(task.taskType), normalizeVideoEditTaskType(taskType)))
                .filter(task -> task.status == FinalCompositionTaskStatus.SUCCESS)
                .count();
        int failed = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> Objects.equals(normalizeVideoEditTaskType(task.taskType), normalizeVideoEditTaskType(taskType)))
                .filter(task -> task.status == FinalCompositionTaskStatus.FAILED)
                .count();
        int active = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> Objects.equals(normalizeVideoEditTaskType(task.taskType), normalizeVideoEditTaskType(taskType)))
                .filter(task -> task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED)
                .count();
        run.totalCount = (int) aggregate.videoEditRenderTasks.stream()
                .filter(task -> Objects.equals(normalizeVideoEditTaskType(task.taskType), normalizeVideoEditTaskType(taskType)))
                .count();
        run.successCount = success;
        run.failedCount = failed;
        run.updatedAt = Instant.now();
        if (active > 0) {
            run.status = PipelineStatus.RUNNING;
            return;
        }
        if (failed == 0 && success == run.totalCount) {
            run.status = PipelineStatus.SUCCESS;
            return;
        }
        if (success > 0 && failed > 0) {
            run.status = PipelineStatus.PARTIAL_FAILED;
            return;
        }
        if (failed > 0) {
            run.status = PipelineStatus.FAILED;
        }
    }

    private void refreshFinalCompositionRun(ScriptProjectAggregate aggregate) {
        PipelineRun run = latestRunByType(aggregate, PipelineType.FINAL_COMPOSITION);
        if (run == null) {
            return;
        }
        int success = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.SUCCESS).count();
        int failed = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.FAILED).count();
        int active = (int) aggregate.finalCompositionTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED)
                .count();
        run.totalCount = aggregate.finalCompositionTasks.size();
        run.successCount = success;
        run.failedCount = failed;
        run.updatedAt = Instant.now();
        if (active > 0) {
            run.status = PipelineStatus.RUNNING;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed == 0 && success == aggregate.finalCompositionTasks.size()) {
            run.status = PipelineStatus.SUCCESS;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (success > 0 && failed > 0) {
            run.status = PipelineStatus.PARTIAL_FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed > 0) {
            run.status = PipelineStatus.FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
        }
    }

    private void refreshExportPackageRun(ScriptProjectAggregate aggregate) {
        PipelineRun run = latestRunByType(aggregate, PipelineType.EXPORT_PACKAGE);
        if (run == null) {
            return;
        }
        int success = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.SUCCESS).count();
        int failed = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.FAILED).count();
        int active = (int) aggregate.exportPackageTasks.stream()
                .filter(task -> task.status == ExportPackageTaskStatus.RUNNING || task.status == ExportPackageTaskStatus.QUEUED)
                .count();
        run.totalCount = aggregate.exportPackageTasks.size();
        run.successCount = success;
        run.failedCount = failed;
        run.updatedAt = Instant.now();
        if (active > 0) {
            run.status = PipelineStatus.RUNNING;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed == 0 && success == aggregate.exportPackageTasks.size()) {
            run.status = PipelineStatus.SUCCESS;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (success > 0 && failed > 0) {
            run.status = PipelineStatus.PARTIAL_FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
            return;
        }
        if (failed > 0) {
            run.status = PipelineStatus.FAILED;
            aggregate.project.status = resolveProjectStatus(aggregate);
        }
    }

    private PipelineRun latestProductionRun(ScriptProjectAggregate aggregate) {
        for (int index = aggregate.pipelineRuns.size() - 1; index >= 0; index--) {
            PipelineRun run = aggregate.pipelineRuns.get(index);
            if (run.pipelineType == PipelineType.VIDEO_GENERATION
                    || run.pipelineType == PipelineType.LIP_SYNC
                    || run.pipelineType == PipelineType.VIDEO_EDIT_PREVIEW
                    || run.pipelineType == PipelineType.VIDEO_EDIT_PUBLISH
                    || run.pipelineType == PipelineType.FINAL_COMPOSITION
                    || run.pipelineType == PipelineType.EXPORT_PACKAGE) {
                return run;
            }
        }
        return null;
    }

    private PipelineRun latestRunByType(ScriptProjectAggregate aggregate, PipelineType pipelineType) {
        for (int index = aggregate.pipelineRuns.size() - 1; index >= 0; index--) {
            PipelineRun run = aggregate.pipelineRuns.get(index);
            if (run.pipelineType == pipelineType) {
                return run;
            }
        }
        return null;
    }

    private VideoSegmentTask findTask(ScriptProjectAggregate aggregate, String segmentTaskId) {
        return aggregate.videoTasks.stream()
                .filter(task -> Objects.equals(task.segmentTaskId, segmentTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "视频任务不存在"));
    }

    private DubbingTask findDubbingTask(ScriptProjectAggregate aggregate, String dubbingTaskId) {
        return aggregate.dubbingTasks.stream()
                .filter(task -> Objects.equals(task.dubbingTaskId, dubbingTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "配音任务不存在"));
    }

    private LipSyncTask findLipSyncTask(ScriptProjectAggregate aggregate, String lipSyncTaskId) {
        return aggregate.lipSyncTasks.stream()
                .filter(task -> Objects.equals(task.lipSyncTaskId, lipSyncTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "口型同步任务不存在"));
    }

    private FinalCompositionTask findFinalCompositionTask(ScriptProjectAggregate aggregate, String finalCompositionTaskId) {
        return aggregate.finalCompositionTasks.stream()
                .filter(task -> Objects.equals(task.finalCompositionTaskId, finalCompositionTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "成片任务不存在"));
    }

    private ExportPackageTask findExportPackageTask(ScriptProjectAggregate aggregate, String exportPackageTaskId) {
        return aggregate.exportPackageTasks.stream()
                .filter(task -> Objects.equals(task.exportPackageTaskId, exportPackageTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "导出包任务不存在"));
    }

    private StoryboardShot findShot(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.shots.stream()
                .filter(shot -> Objects.equals(shot.shotId, shotId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "镜头不存在"));
    }

    private ProjectStatus resolveProjectStatus(ScriptProjectAggregate aggregate) {
        ProjectStatus fallback = aggregate.project.status == null ? ProjectStatus.DRAFT : aggregate.project.status;
        int videoActive = (int) aggregate.videoTasks.stream()
                .filter(task -> task.status == SegmentTaskStatus.RUNNING || task.status == SegmentTaskStatus.QUEUED)
                .count();
        int dubbingActive = (int) aggregate.dubbingTasks.stream()
                .filter(task -> task.status == DubbingTaskStatus.RUNNING || task.status == DubbingTaskStatus.QUEUED)
                .count();
        int lipSyncActive = (int) aggregate.lipSyncTasks.stream()
                .filter(task -> task.status == LipSyncTaskStatus.RUNNING || task.status == LipSyncTaskStatus.QUEUED)
                .count();
        int finalCompositionActive = (int) aggregate.finalCompositionTasks.stream()
                .filter(task -> task.status == FinalCompositionTaskStatus.RUNNING || task.status == FinalCompositionTaskStatus.QUEUED)
                .count();
        int exportPackageActive = (int) aggregate.exportPackageTasks.stream()
                .filter(task -> task.status == ExportPackageTaskStatus.RUNNING || task.status == ExportPackageTaskStatus.QUEUED)
                .count();
        if (videoActive > 0) {
            return ProjectStatus.VIDEO_GENERATING;
        }
        if (dubbingActive > 0) {
            return ProjectStatus.DUBBING_GENERATING;
        }
        if (lipSyncActive > 0) {
            return ProjectStatus.LIP_SYNC_GENERATING;
        }
        if (finalCompositionActive > 0) {
            return ProjectStatus.FINAL_COMPOSITION_GENERATING;
        }
        if (exportPackageActive > 0) {
            return ProjectStatus.EXPORT_PACKAGE_GENERATING;
        }

        int videoSuccess = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.SUCCESS).count();
        int videoFailed = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.FAILED).count();
        int dubbingSuccess = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.SUCCESS).count();
        int dubbingFailed = (int) aggregate.dubbingTasks.stream().filter(task -> task.status == DubbingTaskStatus.FAILED).count();
        int lipSyncSuccess = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.SUCCESS).count();
        int lipSyncFailed = (int) aggregate.lipSyncTasks.stream().filter(task -> task.status == LipSyncTaskStatus.FAILED).count();
        int finalCompositionSuccess = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.SUCCESS).count();
        int finalCompositionFailed = (int) aggregate.finalCompositionTasks.stream().filter(task -> task.status == FinalCompositionTaskStatus.FAILED).count();
        int exportPackageSuccess = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.SUCCESS).count();
        int exportPackageFailed = (int) aggregate.exportPackageTasks.stream().filter(task -> task.status == ExportPackageTaskStatus.FAILED).count();

        if (!aggregate.exportPackageTasks.isEmpty()) {
            if (exportPackageFailed > 0) {
                return exportPackageSuccess > 0 ? ProjectStatus.PARTIAL_FAILED : ProjectStatus.FAILED;
            }
            if (exportPackageSuccess == aggregate.exportPackageTasks.size()) {
                return ProjectStatus.EXPORT_PACKAGE_READY;
            }
        }

        if (!aggregate.finalCompositionTasks.isEmpty()) {
            if (finalCompositionFailed > 0) {
                return finalCompositionSuccess > 0 ? ProjectStatus.PARTIAL_FAILED : ProjectStatus.FAILED;
            }
            if (finalCompositionSuccess == aggregate.finalCompositionTasks.size()) {
                return ProjectStatus.FINAL_COMPOSITION_READY;
            }
        }

        if (!aggregate.videoTasks.isEmpty()) {
            if (videoFailed > 0) {
                return videoSuccess > 0 ? ProjectStatus.PARTIAL_FAILED : ProjectStatus.FAILED;
            }
            if (videoSuccess == aggregate.videoTasks.size()) {
                if (aggregate.dubbingTasks.isEmpty()) {
                    return ProjectStatus.VIDEO_READY;
                }
                if (dubbingFailed > 0) {
                    return dubbingSuccess > 0 ? ProjectStatus.PARTIAL_FAILED : ProjectStatus.FAILED;
                }
                if (dubbingSuccess == aggregate.dubbingTasks.size()) {
                    if (aggregate.lipSyncTasks.isEmpty()) {
                        return ProjectStatus.DUBBING_READY;
                    }
                    if (lipSyncFailed > 0) {
                        return lipSyncSuccess > 0 ? ProjectStatus.PARTIAL_FAILED : ProjectStatus.FAILED;
                    }
                    if (lipSyncSuccess == aggregate.lipSyncTasks.size()) {
                        return ProjectStatus.LIP_SYNC_READY;
                    }
                    return ProjectStatus.DUBBING_READY;
                }
                return ProjectStatus.VIDEO_READY;
            }
        }

        if (!aggregate.dubbingTasks.isEmpty()) {
            if (dubbingFailed > 0) {
                return dubbingSuccess > 0 ? ProjectStatus.PARTIAL_FAILED : ProjectStatus.FAILED;
            }
            if (dubbingSuccess == aggregate.dubbingTasks.size() && fallback == ProjectStatus.DUBBING_GENERATING) {
                return ProjectStatus.SCRIPT_READY;
            }
        }
        return fallback;
    }

    private VideoSegmentTask findSuccessfulVideoTaskForShot(ScriptProjectAggregate aggregate, String shotId) {
        return findSuccessfulVideoTaskForShot(aggregate, shotId, true);
    }

    private VideoSegmentTask findSuccessfulVideoTaskForShot(ScriptProjectAggregate aggregate, String shotId, boolean required) {
        VideoSegmentTask task = aggregate.videoTasks.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .filter(item -> item.status == SegmentTaskStatus.SUCCESS)
                .filter(item -> item.resultVideoFileId != null && !item.resultVideoFileId.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
        if (task == null && required) {
            throw new BizException(400, "镜头缺少可用源视频");
        }
        return task;
    }

    private DubbingTask findSuccessfulDubbingTaskForShot(ScriptProjectAggregate aggregate, String shotId) {
        return findSuccessfulDubbingTaskForShot(aggregate, shotId, true);
    }

    private DubbingTask findSuccessfulDubbingTaskForShot(ScriptProjectAggregate aggregate, String shotId, boolean required) {
        DubbingTask task = aggregate.dubbingTasks.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .filter(item -> item.status == DubbingTaskStatus.SUCCESS)
                .filter(item -> item.resultAudioFileId != null && !item.resultAudioFileId.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
        if (task == null && required) {
            throw new BizException(400, "镜头缺少可用配音音频");
        }
        return task;
    }

    private List<FinalCompositionInputSegment> collectCompositionInputSegments(ScriptProjectAggregate aggregate) {
        List<StoryboardShot> orderedShots = aggregate.shots.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((StoryboardShot shot) -> shot.sequenceNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(shot -> safe(shot.shotId)))
                .toList();
        if (orderedShots.isEmpty()) {
            throw new BizException(400, "请先拆分镜头，再启动成片编排");
        }
        List<FinalCompositionInputSegment> segments = new ArrayList<>();
        List<String> missingShots = new ArrayList<>();
        for (StoryboardShot shot : orderedShots) {
            FinalCompositionInputSegment segment = resolveCompositionInputSegment(aggregate, shot);
            if (segment == null) {
                missingShots.add(shotDisplayName(shot));
                continue;
            }
            segments.add(segment);
        }
        if (!missingShots.isEmpty()) {
            throw new BizException(400, "以下镜头缺少可用于成片编排的结果：" + String.join("、", missingShots));
        }
        if (segments.isEmpty()) {
            throw new BizException(400, "当前项目缺少可用于成片编排的镜头结果，请先生成视频或口型同步结果");
        }
        return segments;
    }

    private FinalCompositionInputSegment resolveCompositionInputSegment(ScriptProjectAggregate aggregate, StoryboardShot shot) {
        LipSyncTask lipSyncTask = findAvailableLipSyncTaskForShot(aggregate, shot.shotId);
        if (lipSyncTask != null) {
            StoredFileRecord lipSyncFile = scriptProjectService.findFile(aggregate, lipSyncTask.resultVideoFileId);
            if (lipSyncFile != null) {
                return toCompositionInputSegment(shot, lipSyncTask.lipSyncTaskId, lipSyncFile, "LIP_SYNC");
            }
        }
        VideoSegmentTask videoTask = findAvailableVideoTaskForShot(aggregate, shot.shotId);
        if (videoTask == null) {
            return null;
        }
        StoredFileRecord videoFile = scriptProjectService.findFile(aggregate, videoTask.resultVideoFileId);
        if (videoFile == null) {
            return null;
        }
        return toCompositionInputSegment(shot, videoTask.segmentTaskId, videoFile, "VIDEO");
    }

    private FinalCompositionInputSegment toCompositionInputSegment(
            StoryboardShot shot,
            String sourceTaskId,
            StoredFileRecord sourceFile,
            String sourceType
    ) {
        FinalCompositionInputSegment segment = new FinalCompositionInputSegment();
        segment.shotId = shot.shotId;
        segment.sequenceNo = shot.sequenceNo;
        segment.shotTitle = shotDisplayName(shot);
        segment.sourceType = sourceType;
        segment.sourceTaskId = sourceTaskId;
        segment.sourceFileId = sourceFile.fileId;
        segment.sourcePublicUrl = sourceFile.publicUrl;
        return segment;
    }

    private VideoSegmentTask findAvailableVideoTaskForShot(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.videoTasks.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .filter(item -> item.status == SegmentTaskStatus.SUCCESS)
                .filter(item -> item.resultVideoFileId != null && !item.resultVideoFileId.isBlank())
                .filter(item -> hasAvailableStoredFile(aggregate, item.resultVideoFileId))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private LipSyncTask findAvailableLipSyncTaskForShot(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.lipSyncTasks.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .filter(item -> item.status == LipSyncTaskStatus.SUCCESS)
                .filter(item -> item.resultVideoFileId != null && !item.resultVideoFileId.isBlank())
                .filter(item -> hasAvailableStoredFile(aggregate, item.resultVideoFileId))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private FinalCompositionTask requireExportSourceFinalCompositionTask(ScriptProjectAggregate aggregate) {
        FinalCompositionTask task = aggregate.finalCompositionTasks.stream()
                .filter(item -> item.status == FinalCompositionTaskStatus.SUCCESS)
                .filter(item -> item.resultVideoFileId != null && !item.resultVideoFileId.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
        if (task == null) {
            throw new BizException(400, "当前项目缺少项目级成片，请先完成成片编排后再生成导出包");
        }
        if (!hasAvailableStoredFile(aggregate, task.resultVideoFileId)) {
            throw new BizException(400, "项目级成片文件已丢失，请重新生成成片后再试");
        }
        return task;
    }

    private ExportSource requireExportSource(ScriptProjectAggregate aggregate) {
        VideoEditRenderTask publishedTask = videoEditingService.latestSuccessfulPublishedTask(aggregate);
        if (publishedTask != null) {
            return new ExportSource(
                    "VIDEO_EDIT_PUBLISHED",
                    publishedTask.renderTaskId,
                    publishedTask.draftVersion,
                    publishedTask.resultVideoFileId,
                    null,
                    copyVideoEditSegments(publishedTask.inputSegments)
            );
        }
        FinalCompositionTask finalCompositionTask = requireExportSourceFinalCompositionTask(aggregate);
        return new ExportSource(
                "FINAL_COMPOSITION",
                finalCompositionTask.finalCompositionTaskId,
                null,
                finalCompositionTask.resultVideoFileId,
                finalCompositionTask.inputSegments == null ? List.of() : new ArrayList<>(finalCompositionTask.inputSegments),
                null
        );
    }

    private void applyExportSource(ExportPackageTask task, ExportSource source) {
        task.sourceVideoOriginType = source.originType();
        task.sourceVideoEditRenderTaskId = "VIDEO_EDIT_PUBLISHED".equals(source.originType()) ? source.taskId() : null;
        task.sourceVideoEditDraftVersion = "VIDEO_EDIT_PUBLISHED".equals(source.originType()) ? source.draftVersion() : null;
        task.sourceFinalCompositionTaskId = "FINAL_COMPOSITION".equals(source.originType()) ? source.taskId() : null;
        task.sourceFinalVideoFileId = source.videoFileId();
    }

    private boolean hasAvailableStoredFile(ScriptProjectAggregate aggregate, String fileId) {
        StoredFileRecord record = scriptProjectService.findFile(aggregate, fileId);
        return record != null && localAssetFileService.exists(record);
    }

    private Map<String, Object> buildExportPackageManifest(
            ScriptProjectAggregate aggregate,
            ExportPackageTask task,
            ExportSource source,
            StoredFileRecord finalVideo
    ) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("packageType", "SCRIPT_PROJECT_EXPORT");
        manifest.put("packageVersion", "1.0");
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("exportPackageTaskId", task.exportPackageTaskId);
        manifest.put("sourceTaskId", source.taskId());
        manifest.put("sourceFinalCompositionTaskId", "FINAL_COMPOSITION".equals(source.originType()) ? source.taskId() : null);
        manifest.put("sourceVideoOriginType", source.originType());
        manifest.put("sourceVideoEditDraftVersion", source.draftVersion());

        Map<String, Object> project = new LinkedHashMap<>();
        project.put("projectId", aggregate.project.projectId);
        project.put("projectName", aggregate.project.name);
        project.put("language", aggregate.project.language);
        project.put("aspectRatio", aggregate.project.aspectRatio);
        project.put("targetDuration", aggregate.project.targetDuration);
        project.put("shotCount", aggregate.shots.size());
        manifest.put("project", project);

        String packagedVideoName = safeFileName(aggregate.project.name, "script-project") + "-final.mp4";
        List<Map<String, Object>> files = new ArrayList<>();
        Map<String, Object> packagedVideo = toFileDescriptor(finalVideo);
        packagedVideo.put("role", "project_final_video");
        packagedVideo.put("packagePath", "media/" + packagedVideoName);
        files.add(packagedVideo);
        Map<String, Object> packagedManifest = new LinkedHashMap<>();
        packagedManifest.put("role", "manifest");
        packagedManifest.put("packagePath", "manifest.json");
        packagedManifest.put("mediaType", "application/json");
        files.add(packagedManifest);
        manifest.put("files", files);

        Map<String, StoryboardShot> shotMap = new LinkedHashMap<>();
        for (StoryboardShot shot : aggregate.shots) {
            if (shot != null && shot.shotId != null) {
                shotMap.put(shot.shotId, shot);
            }
        }
        List<Map<String, Object>> shotSummaries = new ArrayList<>();
        List<FinalCompositionInputSegment> inputSegments = source.finalCompositionSegments() == null
                ? List.of()
                : source.finalCompositionSegments().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((FinalCompositionInputSegment item) -> item.sequenceNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(item -> safe(item.shotId)))
                .toList();
        for (FinalCompositionInputSegment segment : inputSegments) {
            StoryboardShot shot = shotMap.get(segment.shotId);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("shotId", segment.shotId);
            summary.put("sequenceNo", segment.sequenceNo);
            summary.put("shotTitle", firstNonBlank(segment.shotTitle, shotDisplayName(shot)));
            summary.put("sourceType", segment.sourceType);
            summary.put("sourceTaskId", segment.sourceTaskId);
            summary.put("sourceFileId", segment.sourceFileId);
            summary.put("sourcePublicUrl", segment.sourcePublicUrl);
            summary.put("scriptText", shot == null ? "" : safe(shot.scriptText));
            summary.put("actionSummary", shot == null ? "" : safe(shot.actionSummary));
            summary.put("cameraMovement", shot == null ? "" : firstNonBlankValue(shot.cameraMovement, shot.cameraMove));
            shotSummaries.add(summary);
        }
        if (inputSegments.isEmpty() && source.videoEditSegments() != null) {
            source.videoEditSegments().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator
                            .comparing((VideoEditSegment item) -> item.sequenceNo, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(item -> safe(item.segmentId)))
                    .forEach(segment -> {
                        StoryboardShot shot = shotMap.get(segment.shotId);
                        Map<String, Object> summary = new LinkedHashMap<>();
                        summary.put("shotId", segment.shotId);
                        summary.put("sequenceNo", segment.sequenceNo);
                        summary.put("shotTitle", shotDisplayName(shot));
                        summary.put("sourceType", segment.sourceType);
                        summary.put("sourceTaskId", segment.sourceTaskId);
                        summary.put("sourceFileId", segment.sourceFileId);
                        summary.put("sourcePublicUrl", segment.sourcePublicUrl);
                        summary.put("trimInMs", segment.trimInMs);
                        summary.put("trimOutMs", segment.trimOutMs);
                        summary.put("transitionMode", segment.transitionMode);
                        summary.put("scriptText", shot == null ? "" : safe(shot.scriptText));
                        summary.put("actionSummary", shot == null ? "" : safe(shot.actionSummary));
                        summary.put("cameraMovement", shot == null ? "" : firstNonBlankValue(shot.cameraMovement, shot.cameraMove));
                        shotSummaries.add(summary);
                    });
        }
        manifest.put("shots", shotSummaries);
        return manifest;
    }

    private byte[] buildExportPackageArchive(
            StoredFileRecord manifestFile,
            StoredFileRecord finalVideo,
            String packagedVideoName
    ) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                addZipEntry(zipOutputStream, "manifest.json", localAssetFileService.readBytes(manifestFile));
                addZipEntry(zipOutputStream, "media/" + safeFileName(packagedVideoName, "project-final.mp4"), localAssetFileService.readBytes(finalVideo));
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("构建导出包归档失败", ex);
        }
    }

    private void addZipEntry(ZipOutputStream output, String entryName, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        output.putNextEntry(entry);
        output.write(bytes == null ? new byte[0] : bytes);
        output.closeEntry();
    }

    private void applyStoredFileMetadata(
            ExportPackageTask task,
            StoredFileRecord manifestFile,
            StoredFileRecord archiveFile
    ) {
        task.manifestStorageProvider = manifestFile.storageProvider;
        task.manifestBucketName = manifestFile.bucketName;
        task.manifestObjectKey = manifestFile.objectKey;
        task.manifestPublicUrl = manifestFile.publicUrl;
        task.archiveStorageProvider = archiveFile.storageProvider;
        task.archiveBucketName = archiveFile.bucketName;
        task.archiveObjectKey = archiveFile.objectKey;
        task.archivePublicUrl = archiveFile.publicUrl;
    }

    private Map<String, Object> toFileDescriptor(StoredFileRecord record) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("fileId", record.fileId);
        descriptor.put("fileName", record.fileName);
        descriptor.put("relativePath", record.relativePath);
        descriptor.put("storageProvider", record.storageProvider == null ? null : record.storageProvider.name());
        descriptor.put("bucketName", record.bucketName);
        descriptor.put("objectKey", record.objectKey);
        descriptor.put("publicUrl", record.publicUrl);
        descriptor.put("mediaType", record.mediaType);
        descriptor.put("sizeBytes", record.sizeBytes);
        descriptor.put("createdAt", record.createdAt == null ? null : record.createdAt.toString());
        return descriptor;
    }

    private StoredFileRecord requireStoredFile(ScriptProjectAggregate aggregate, String fileId, String label) {
        StoredFileRecord record = scriptProjectService.findFile(aggregate, fileId);
        if (record == null) {
            throw new BizException(400, label + "不存在，请重新生成后再试");
        }
        if (!localAssetFileService.exists(record)) {
            throw new BizException(400, label + "文件已丢失，请重新生成后再试");
        }
        return record;
    }

    private List<VideoEditSegment> copyVideoEditSegments(List<VideoEditSegment> segments) {
        List<VideoEditSegment> copies = new ArrayList<>();
        if (segments == null) {
            return copies;
        }
        for (VideoEditSegment item : segments) {
            if (item == null) {
                continue;
            }
            VideoEditSegment copy = new VideoEditSegment();
            copy.segmentId = item.segmentId;
            copy.shotId = item.shotId;
            copy.sequenceNo = item.sequenceNo;
            copy.enabled = item.enabled;
            copy.sourceType = item.sourceType;
            copy.sourceTaskId = item.sourceTaskId;
            copy.sourceFileId = item.sourceFileId;
            copy.sourcePublicUrl = item.sourcePublicUrl;
            copy.sourceDurationMs = item.sourceDurationMs;
            copy.trimInMs = item.trimInMs;
            copy.trimOutMs = item.trimOutMs;
            copy.transitionMode = item.transitionMode;
            copy.trackKey = item.trackKey;
            copy.extensions = item.extensions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.extensions);
            copies.add(copy);
        }
        return copies;
    }

    private PipelineType pipelineTypeForVideoEditTask(String taskType) {
        return VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(taskType)
                ? PipelineType.VIDEO_EDIT_PUBLISH
                : PipelineType.VIDEO_EDIT_PREVIEW;
    }

    private String currentStageForVideoEditTask(String taskType) {
        return VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(taskType)
                ? "VIDEO_EDIT_PUBLISHING"
                : "VIDEO_EDIT_PREVIEW_RENDERING";
    }

    private String normalizeVideoEditTaskType(String taskType) {
        return VideoEditingService.TASK_TYPE_PUBLISH.equalsIgnoreCase(taskType)
                ? VideoEditingService.TASK_TYPE_PUBLISH
                : VideoEditingService.TASK_TYPE_PREVIEW;
    }

    private String shotDisplayName(StoryboardShot shot) {
        String title = safe(shot == null ? null : shot.title).trim();
        if (!title.isBlank()) {
            return title;
        }
        return safe(shot == null ? null : shot.shotId);
    }

    private String safeFileName(String raw, String fallback) {
        String normalized = safe(raw).trim().replaceAll("[^a-zA-Z0-9._-\\u4e00-\\u9fa5]", "_");
        return normalized.isBlank() ? fallback : normalized;
    }

    private Double normalizeSpeechRate(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0d;
        }
        return Math.max(0.5d, Math.min(2.0d, value));
    }

    private String firstNonBlankValue(String... values) {
        String resolved = firstNonBlank(values);
        return resolved == null ? "" : resolved;
    }

    private byte[] synthesizeDubbingAudioBytes(String text, String voiceName, Double speechRate) {
        int sampleRate = 22050;
        double normalizedRate = normalizeSpeechRate(speechRate);
        double durationSeconds = Math.max(1.2d, Math.min(8.0d, (1.0d + safe(text).length() / 18.0d) / normalizedRate));
        int totalSamples = Math.max(1, (int) (sampleRate * durationSeconds));
        double frequency = 220.0d + Math.abs(Objects.hashCode(safe(voiceName)) % 180);
        byte[] pcm = new byte[totalSamples * 2];
        for (int index = 0; index < totalSamples; index++) {
            double position = (double) index / sampleRate;
            double envelope = Math.min(1.0d, index / (sampleRate * 0.05d))
                    * Math.min(1.0d, (totalSamples - index) / (sampleRate * 0.08d));
            double modulation = 0.65d + 0.35d * Math.sin(2 * Math.PI * 2.0d * position);
            short sample = (short) (Math.sin(2 * Math.PI * frequency * position) * 12000 * envelope * modulation);
            pcm[index * 2] = (byte) (sample & 0xff);
            pcm[index * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return wrapAsWave(sampleRate, (short) 1, (short) 16, pcm);
    }

    private byte[] wrapAsWave(int sampleRate, short channels, short bitsPerSample, byte[] pcm) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        short blockAlign = (short) (channels * bitsPerSample / 8);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(44 + pcm.length);
            output.write("RIFF".getBytes());
            writeIntLE(output, 36 + pcm.length);
            output.write("WAVE".getBytes());
            output.write("fmt ".getBytes());
            writeIntLE(output, 16);
            writeShortLE(output, (short) 1);
            writeShortLE(output, channels);
            writeIntLE(output, sampleRate);
            writeIntLE(output, byteRate);
            writeShortLE(output, blockAlign);
            writeShortLE(output, bitsPerSample);
            output.write("data".getBytes());
            writeIntLE(output, pcm.length);
            output.write(pcm);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("构造配音音频失败", ex);
        }
    }

    private void writeIntLE(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private void writeShortLE(ByteArrayOutputStream output, short value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }

    private String parseVideoUrl(Map<String, Object> body) {
        Set<String> urls = new LinkedHashSet<>();
        collectUrls(body, urls, 0);
        return urls.stream().findFirst().orElse(null);
    }

    private void collectUrls(Object node, Set<String> urls, int depth) {
        if (node == null || depth > 8) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).toLowerCase();
                Object value = entry.getValue();
                if (key.contains("url")) {
                    String maybe = safe(value == null ? null : String.valueOf(value));
                    if (maybe.startsWith("http://") || maybe.startsWith("https://")) {
                        urls.add(maybe);
                    }
                }
                if (value instanceof Map<?, ?> || value instanceof List<?>) {
                    collectUrls(value, urls, depth + 1);
                }
            }
            return;
        }
        if (node instanceof List<?> list) {
            list.forEach(item -> collectUrls(item, urls, depth + 1));
        }
    }

    private String parseTaskId(Map<String, Object> body) {
        return firstNonBlank(
                valueAsString(body.get("task_id")),
                valueAsString(body.get("taskId")),
                valueAsString(body.get("id")),
                nestedValue(body, "data", "task_id"),
                nestedValue(body, "data", "id"),
                nestedValue(body, "result", "task_id")
        );
    }

    private String parseTaskStatus(Map<String, Object> body) {
        return firstNonBlank(
                valueAsString(body.get("status")),
                nestedValue(body, "output", "status"),
                nestedValue(body, "data", "status"),
                nestedValue(body, "result", "status")
        );
    }

    private String parseTaskError(Map<String, Object> body) {
        return firstNonBlank(
                valueAsString(body.get("message")),
                valueAsString(body.get("error")),
                nestedValue(body, "data", "message"),
                "未知错误"
        );
    }

    private boolean isFailedStatus(String status) {
        String normalized = safe(status).toLowerCase();
        return normalized.equals("failed") || normalized.equals("error") || normalized.equals("timeout") || normalized.equals("cancelled");
    }

    private String nestedValue(Map<?, ?> root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(key);
        }
        return valueAsString(current);
    }

    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        return raw.isEmpty() ? null : raw;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean containsAny(List<String> values, String target) {
        return values != null && values.contains(target);
    }

    private Object lock(String projectId) {
        return projectLocks.computeIfAbsent(projectId, key -> new Object());
    }

    private String nextId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ExportSource(
            String originType,
            String taskId,
            Integer draftVersion,
            String videoFileId,
            List<FinalCompositionInputSegment> finalCompositionSegments,
            List<VideoEditSegment> videoEditSegments
    ) {
    }

    private record VideoCallResult(String videoUrl, String providerTaskId) {
    }
}
