package com.example.aigc.service;

import com.example.aigc.config.PipelineVideoProperties;
import com.example.aigc.dto.PipelineStatusData;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.PipelineRun;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.enums.PipelineStatus;
import com.example.aigc.enums.PipelineType;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.SegmentTaskStatus;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.model.WorkflowModelKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
    private final ConcurrentHashMap<String, Object> projectLocks = new ConcurrentHashMap<>();

    public ScriptProductionOrchestrator(
            ScriptProjectService scriptProjectService,
            PromptTemplateService promptTemplateService,
            AiCapabilityRoutingService aiCapabilityRoutingService,
            ProviderHttpGateway providerHttpGateway,
            LocalAssetFileService localAssetFileService,
            PipelineVideoProperties pipelineVideoProperties,
            @Qualifier("videoPipelineExecutor") Executor videoPipelineExecutor,
            VideoStylePresetRegistry videoStylePresetRegistry
    ) {
        this.scriptProjectService = scriptProjectService;
        this.promptTemplateService = promptTemplateService;
        this.aiCapabilityRoutingService = aiCapabilityRoutingService;
        this.providerHttpGateway = providerHttpGateway;
        this.localAssetFileService = localAssetFileService;
        this.pipelineVideoProperties = pipelineVideoProperties;
        this.videoPipelineExecutor = videoPipelineExecutor;
        this.videoStylePresetRegistry = videoStylePresetRegistry;
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
            task.retryCount = nextRetry;
            task.status = SegmentTaskStatus.QUEUED;
            task.errorMessage = null;
            task.resultVideoFileId = null;
            task.providerTaskId = null;
            task.requestPayloadFileId = null;
            task.startedAt = null;
            task.finishedAt = null;
            aggregate.project.status = ProjectStatus.VIDEO_GENERATING;

            PipelineRun run = latestVideoRun(aggregate);
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

    public PipelineStatusData getPipelineStatus(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        PipelineStatusData data = new PipelineStatusData();
        data.projectId = projectId;
        data.projectStatus = aggregate.project.status;
        data.latestRun = latestVideoRun(aggregate);
        data.totalCount = aggregate.videoTasks.size();
        data.successCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.SUCCESS).count();
        data.failedCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.FAILED).count();
        data.runningCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.RUNNING).count();
        data.queuedCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.QUEUED).count();
        data.pendingCount = (int) aggregate.videoTasks.stream().filter(task -> task.status == SegmentTaskStatus.PENDING).count();
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
        String prompt = promptTemplateService.render(
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
        PipelineRun run = latestVideoRun(aggregate);
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
            aggregate.project.status = ProjectStatus.VIDEO_GENERATING;
            return;
        }
        if (failed == 0 && success == aggregate.videoTasks.size()) {
            run.status = PipelineStatus.SUCCESS;
            aggregate.project.status = ProjectStatus.COMPLETED;
            return;
        }
        if (success > 0 && failed > 0) {
            run.status = PipelineStatus.PARTIAL_FAILED;
            aggregate.project.status = ProjectStatus.PARTIAL_FAILED;
            return;
        }
        if (failed > 0) {
            run.status = PipelineStatus.FAILED;
            aggregate.project.status = ProjectStatus.FAILED;
        }
    }

    private PipelineRun latestVideoRun(ScriptProjectAggregate aggregate) {
        for (int index = aggregate.pipelineRuns.size() - 1; index >= 0; index--) {
            PipelineRun run = aggregate.pipelineRuns.get(index);
            if (run.pipelineType == PipelineType.VIDEO_GENERATION) {
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

    private StoryboardShot findShot(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.shots.stream()
                .filter(shot -> Objects.equals(shot.shotId, shotId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "镜头不存在"));
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

    private record VideoCallResult(String videoUrl, String providerTaskId) {
    }
}
