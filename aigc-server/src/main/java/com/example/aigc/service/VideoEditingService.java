package com.example.aigc.service;

import com.example.aigc.dto.VideoEditDraftResponse;
import com.example.aigc.dto.VideoEditDraftSaveRequest;
import com.example.aigc.entity.LipSyncTask;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.VideoEditDraft;
import com.example.aigc.entity.VideoEditRenderTask;
import com.example.aigc.entity.VideoEditSegment;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.enums.FinalCompositionTaskStatus;
import com.example.aigc.enums.LipSyncTaskStatus;
import com.example.aigc.enums.SegmentTaskStatus;
import com.example.aigc.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class VideoEditingService {

    public static final String TASK_TYPE_PREVIEW = "PREVIEW";
    public static final String TASK_TYPE_PUBLISH = "PUBLISH";
    public static final String SOURCE_TYPE_VIDEO = "VIDEO";
    public static final String SOURCE_TYPE_LIP_SYNC = "LIP_SYNC";

    private final ScriptProjectService scriptProjectService;
    private final LocalAssetFileService localAssetFileService;

    public VideoEditingService(
            ScriptProjectService scriptProjectService,
            LocalAssetFileService localAssetFileService
    ) {
        this.scriptProjectService = scriptProjectService;
        this.localAssetFileService = localAssetFileService;
    }

    public VideoEditDraftResponse getDraft(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        VideoEditDraft draft = ensureDraft(aggregate, true);
        if (draft.createdAt == null) {
            draft.createdAt = Instant.now();
        }
        if (draft.updatedAt == null) {
            draft.updatedAt = draft.createdAt;
        }
        scriptProjectService.save(aggregate);
        return toResponse(aggregate, draft);
    }

    public VideoEditDraftResponse saveDraft(String projectId, VideoEditDraftSaveRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        VideoEditDraft draft = ensureDraft(aggregate, true);
        Integer currentVersion = safeVersion(draft.version);
        if (request != null && request.expectedVersion != null && !Objects.equals(request.expectedVersion, currentVersion)) {
            throw new BizException(409, "剪辑草稿版本已变更，请刷新后重试");
        }
        List<VideoEditSegment> normalizedSegments = normalizeSegments(
                aggregate,
                request == null ? List.of() : request.segments,
                true
        );
        draft.segments = normalizedSegments;
        draft.extensions = sanitizeExtensions(request == null ? null : request.extensions);
        draft.version = currentVersion + 1;
        draft.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return toResponse(aggregate, draft);
    }

    public VideoEditDraftResponse resetDraft(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        VideoEditDraft draft = ensureDraft(aggregate, true);
        draft.segments = buildDefaultSegments(aggregate);
        draft.extensions = defaultDraftExtensions();
        draft.version = safeVersion(draft.version) + 1;
        draft.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return toResponse(aggregate, draft);
    }

    public List<VideoEditRenderTask> listRenderTasks(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        return aggregate.videoEditRenderTasks.stream()
                .sorted(Comparator.comparing(
                        (VideoEditRenderTask task) -> task.createdAt == null ? task.startedAt : task.createdAt,
                        Comparator.nullsLast(Instant::compareTo)
                ).reversed())
                .toList();
    }

    public VideoEditDraft requireRenderableDraft(ScriptProjectAggregate aggregate) {
        VideoEditDraft draft = ensureDraft(aggregate, true);
        draft.segments = normalizeSegments(aggregate, draft.segments, true);
        if (draft.updatedAt == null) {
            draft.updatedAt = Instant.now();
        }
        return draft;
    }

    public VideoEditDraft getDraftOrNull(ScriptProjectAggregate aggregate) {
        return aggregate.videoEditDraft;
    }

    public VideoEditDraftResponse toResponse(ScriptProjectAggregate aggregate, VideoEditDraft draft) {
        VideoEditDraftResponse response = new VideoEditDraftResponse();
        response.projectId = aggregate.project.projectId;
        response.draftId = draft.draftId;
        response.version = safeVersion(draft.version);
        response.publishedVersion = draft.publishedVersion;
        response.publishedAt = draft.publishedAt;
        response.publishedRenderTaskId = draft.publishedRenderTaskId;
        response.latestPreviewTaskId = draft.latestPreviewTaskId;
        response.latestPublishTaskId = draft.latestPublishTaskId;
        response.hasPublishedResult = hasPublishedRender(aggregate, draft);
        response.hasUnpublishedChanges = response.version > safeVersion(draft.publishedVersion);
        response.segments = copySegments(draft.segments);
        response.extensions = sanitizeExtensions(draft.extensions);
        return response;
    }

    public VideoEditRenderTask findRenderTask(ScriptProjectAggregate aggregate, String renderTaskId) {
        return aggregate.videoEditRenderTasks.stream()
                .filter(task -> Objects.equals(task.renderTaskId, renderTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "剪辑渲染任务不存在"));
    }

    public VideoEditRenderTask latestSuccessfulPublishedTask(ScriptProjectAggregate aggregate) {
        VideoEditDraft draft = aggregate.videoEditDraft;
        for (int index = aggregate.videoEditRenderTasks.size() - 1; index >= 0; index--) {
            VideoEditRenderTask task = aggregate.videoEditRenderTasks.get(index);
            if (task == null
                    || task.status != FinalCompositionTaskStatus.SUCCESS
                    || task.resultVideoFileId == null
                    || task.resultVideoFileId.isBlank()) {
                continue;
            }
            boolean published = task.publishedAt != null
                    || (draft != null && Objects.equals(draft.publishedRenderTaskId, task.renderTaskId));
            if (!published) {
                continue;
            }
            StoredFileRecord file = scriptProjectService.findFile(aggregate, task.resultVideoFileId);
            if (file != null && localAssetFileService.exists(file)) {
                return task;
            }
        }
        return null;
    }

    private VideoEditDraft ensureDraft(ScriptProjectAggregate aggregate, boolean createIfMissing) {
        if (aggregate.videoEditDraft != null) {
            return aggregate.videoEditDraft;
        }
        if (!createIfMissing) {
            return null;
        }
        List<VideoEditSegment> defaultSegments = buildDefaultSegments(aggregate);
        VideoEditDraft draft = new VideoEditDraft();
        draft.projectId = aggregate.project.projectId;
        draft.draftId = nextId("draft");
        draft.version = 1;
        draft.segments = defaultSegments;
        draft.extensions = defaultDraftExtensions();
        draft.createdAt = Instant.now();
        draft.updatedAt = draft.createdAt;
        aggregate.videoEditDraft = draft;
        return draft;
    }

    private List<VideoEditSegment> buildDefaultSegments(ScriptProjectAggregate aggregate) {
        List<StoryboardShot> orderedShots = aggregate.shots.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((StoryboardShot shot) -> shot.sequenceNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(shot -> safe(shot.shotId)))
                .toList();
        List<VideoEditSegment> segments = new ArrayList<>();
        for (StoryboardShot shot : orderedShots) {
            VideoEditSegment segment = resolveDefaultSegment(aggregate, shot);
            if (segment != null) {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            throw new BizException(400, "当前项目暂无可用于剪辑的片段，请先生成镜头视频或口型同步结果");
        }
        return segments;
    }

    private VideoEditSegment resolveDefaultSegment(ScriptProjectAggregate aggregate, StoryboardShot shot) {
        LipSyncTask lipSyncTask = latestAvailableLipSyncTask(aggregate, shot.shotId);
        if (lipSyncTask != null) {
            StoredFileRecord sourceFile = requireFile(aggregate, lipSyncTask.resultVideoFileId, shotDisplayName(shot));
            return toVideoEditSegment(shot, null, null, null, SOURCE_TYPE_LIP_SYNC, lipSyncTask.lipSyncTaskId, sourceFile);
        }
        VideoSegmentTask videoTask = latestAvailableVideoTask(aggregate, shot.shotId);
        if (videoTask == null) {
            return null;
        }
        StoredFileRecord sourceFile = requireFile(aggregate, videoTask.resultVideoFileId, shotDisplayName(shot));
        return toVideoEditSegment(shot, null, null, null, SOURCE_TYPE_VIDEO, videoTask.segmentTaskId, sourceFile);
    }

    private List<VideoEditSegment> normalizeSegments(
            ScriptProjectAggregate aggregate,
            List<VideoEditSegment> incomingSegments,
            boolean requireEnabledSegments
    ) {
        if (incomingSegments == null || incomingSegments.isEmpty()) {
            throw new BizException(400, "剪辑草稿至少需要一个片段");
        }
        Map<String, StoryboardShot> shotMap = new LinkedHashMap<>();
        for (StoryboardShot shot : aggregate.shots) {
            if (shot != null && shot.shotId != null) {
                shotMap.put(shot.shotId, shot);
            }
        }

        List<VideoEditSegment> normalized = new ArrayList<>();
        int enabledCount = 0;
        int order = 1;
        for (VideoEditSegment incoming : incomingSegments) {
            if (incoming == null || safe(incoming.shotId).isBlank()) {
                throw new BizException(400, "剪辑片段缺少镜头标识");
            }
            StoryboardShot shot = shotMap.get(incoming.shotId);
            if (shot == null) {
                throw new BizException(400, "镜头不存在或已被删除：" + incoming.shotId);
            }
            boolean enabled = incoming.enabled == null || Boolean.TRUE.equals(incoming.enabled);
            VideoEditSegment segment = resolveSegmentBySourceType(
                    aggregate,
                    shot,
                    normalizeSourceType(incoming.sourceType),
                    incoming,
                    enabled,
                    order
            );
            validateTrim(segment, shotDisplayName(shot));
            if (Boolean.TRUE.equals(segment.enabled)) {
                enabledCount++;
            }
            normalized.add(segment);
            order++;
        }
        if (requireEnabledSegments && enabledCount == 0) {
            throw new BizException(400, "剪辑草稿至少需要一个启用片段");
        }
        return normalized;
    }

    private VideoEditSegment resolveSegmentBySourceType(
            ScriptProjectAggregate aggregate,
            StoryboardShot shot,
            String sourceType,
            VideoEditSegment incoming,
            Boolean enabled,
            Integer order
    ) {
        String normalizedType = normalizeSourceType(sourceType);
        if (SOURCE_TYPE_LIP_SYNC.equals(normalizedType)) {
            LipSyncTask task = latestAvailableLipSyncTask(aggregate, shot.shotId);
            if (task == null) {
                throw new BizException(400, "镜头缺少可用的口型同步结果：" + shotDisplayName(shot));
            }
            StoredFileRecord sourceFile = requireFile(aggregate, task.resultVideoFileId, shotDisplayName(shot));
            return toVideoEditSegment(shot, incoming, enabled, order, normalizedType, task.lipSyncTaskId, sourceFile);
        }
        VideoSegmentTask task = latestAvailableVideoTask(aggregate, shot.shotId);
        if (task == null) {
            throw new BizException(400, "镜头缺少可用的视频结果：" + shotDisplayName(shot));
        }
        StoredFileRecord sourceFile = requireFile(aggregate, task.resultVideoFileId, shotDisplayName(shot));
        return toVideoEditSegment(shot, incoming, enabled, order, SOURCE_TYPE_VIDEO, task.segmentTaskId, sourceFile);
    }

    private VideoEditSegment toVideoEditSegment(
            StoryboardShot shot,
            VideoEditSegment incoming,
            Boolean enabled,
            Integer order,
            String sourceType,
            String sourceTaskId,
            StoredFileRecord sourceFile
    ) {
        long durationMs = estimateDurationMs(shot);
        VideoEditSegment segment = new VideoEditSegment();
        segment.segmentId = incoming != null && incoming.segmentId != null && !incoming.segmentId.isBlank()
                ? incoming.segmentId
                : nextId("clip");
        segment.shotId = shot.shotId;
        segment.sequenceNo = order == null ? shot.sequenceNo : order;
        segment.enabled = enabled == null ? Boolean.TRUE : enabled;
        segment.sourceType = sourceType;
        segment.sourceTaskId = sourceTaskId;
        segment.sourceFileId = sourceFile.fileId;
        segment.sourcePublicUrl = sourceFile.publicUrl;
        segment.sourceDurationMs = durationMs;
        segment.trimInMs = normalizeTrimStart(incoming == null ? null : incoming.trimInMs);
        segment.trimOutMs = normalizeTrimEnd(incoming == null ? null : incoming.trimOutMs, durationMs);
        segment.transitionMode = defaultIfBlank(incoming == null ? null : incoming.transitionMode, "CUT");
        segment.trackKey = defaultIfBlank(incoming == null ? null : incoming.trackKey, "video-main");
        segment.extensions = sanitizeExtensions(incoming == null ? null : incoming.extensions);
        return segment;
    }

    private void validateTrim(VideoEditSegment segment, String shotName) {
        long durationMs = segment.sourceDurationMs == null ? 0L : segment.sourceDurationMs;
        long trimInMs = segment.trimInMs == null ? 0L : segment.trimInMs;
        long trimOutMs = segment.trimOutMs == null ? durationMs : segment.trimOutMs;
        if (trimInMs < 0L) {
            throw new BizException(400, "片段裁切起点不能小于 0：" + shotName);
        }
        if (trimOutMs <= trimInMs) {
            throw new BizException(400, "片段裁切结束时间必须晚于开始时间：" + shotName);
        }
        if (durationMs > 0L && trimOutMs > durationMs) {
            throw new BizException(400, "片段裁切区间超出来源时长：" + shotName);
        }
    }

    private long estimateDurationMs(StoryboardShot shot) {
        Integer seconds = shot == null || shot.targetDurationSec == null || shot.targetDurationSec <= 0
                ? null
                : shot.targetDurationSec;
        if (seconds == null || seconds <= 0) {
            seconds = 5;
        }
        return seconds.longValue() * 1000L;
    }

    private long normalizeTrimStart(Long trimInMs) {
        if (trimInMs == null) {
            return 0L;
        }
        return Math.max(0L, trimInMs);
    }

    private long normalizeTrimEnd(Long trimOutMs, long durationMs) {
        if (trimOutMs == null || trimOutMs <= 0L) {
            return durationMs;
        }
        return trimOutMs;
    }

    private StoredFileRecord requireFile(ScriptProjectAggregate aggregate, String fileId, String shotName) {
        StoredFileRecord record = scriptProjectService.findFile(aggregate, fileId);
        if (record == null || !localAssetFileService.exists(record)) {
            throw new BizException(400, "片段来源文件已丢失：" + shotName);
        }
        return record;
    }

    private VideoSegmentTask latestAvailableVideoTask(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.videoTasks.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .filter(item -> item.status == SegmentTaskStatus.SUCCESS)
                .filter(item -> item.resultVideoFileId != null && !item.resultVideoFileId.isBlank())
                .filter(item -> hasAvailableFile(aggregate, item.resultVideoFileId))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private LipSyncTask latestAvailableLipSyncTask(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.lipSyncTasks.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .filter(item -> item.status == LipSyncTaskStatus.SUCCESS)
                .filter(item -> item.resultVideoFileId != null && !item.resultVideoFileId.isBlank())
                .filter(item -> hasAvailableFile(aggregate, item.resultVideoFileId))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private boolean hasAvailableFile(ScriptProjectAggregate aggregate, String fileId) {
        StoredFileRecord file = scriptProjectService.findFile(aggregate, fileId);
        return file != null && localAssetFileService.exists(file);
    }

    private boolean hasPublishedRender(ScriptProjectAggregate aggregate, VideoEditDraft draft) {
        if (draft == null || draft.publishedRenderTaskId == null || draft.publishedRenderTaskId.isBlank()) {
            return false;
        }
        VideoEditRenderTask task = aggregate.videoEditRenderTasks.stream()
                .filter(item -> Objects.equals(item.renderTaskId, draft.publishedRenderTaskId))
                .findFirst()
                .orElse(null);
        if (task == null || task.status != FinalCompositionTaskStatus.SUCCESS) {
            return false;
        }
        StoredFileRecord file = scriptProjectService.findFile(aggregate, task.resultVideoFileId);
        return file != null && localAssetFileService.exists(file);
    }

    private List<VideoEditSegment> copySegments(List<VideoEditSegment> segments) {
        List<VideoEditSegment> copies = new ArrayList<>();
        if (segments == null) {
            return copies;
        }
        for (VideoEditSegment segment : segments) {
            if (segment == null) {
                continue;
            }
            VideoEditSegment copy = new VideoEditSegment();
            copy.segmentId = segment.segmentId;
            copy.shotId = segment.shotId;
            copy.sequenceNo = segment.sequenceNo;
            copy.enabled = segment.enabled;
            copy.sourceType = segment.sourceType;
            copy.sourceTaskId = segment.sourceTaskId;
            copy.sourceFileId = segment.sourceFileId;
            copy.sourcePublicUrl = segment.sourcePublicUrl;
            copy.sourceDurationMs = segment.sourceDurationMs;
            copy.trimInMs = segment.trimInMs;
            copy.trimOutMs = segment.trimOutMs;
            copy.transitionMode = segment.transitionMode;
            copy.trackKey = segment.trackKey;
            copy.extensions = sanitizeExtensions(segment.extensions);
            copies.add(copy);
        }
        return copies;
    }

    private Map<String, Object> sanitizeExtensions(Map<String, Object> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(extensions);
    }

    private Map<String, Object> defaultDraftExtensions() {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("timelineMode", "SINGLE_TRACK");
        extensions.put("reservedCapabilities", List.of("multi_track_audio", "subtitle_burn", "intro_outro_template"));
        return extensions;
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = safe(sourceType).trim().toUpperCase();
        if (normalized.isBlank()) {
            return SOURCE_TYPE_LIP_SYNC;
        }
        if (SOURCE_TYPE_LIP_SYNC.equals(normalized) || SOURCE_TYPE_VIDEO.equals(normalized)) {
            return normalized;
        }
        throw new BizException(400, "不支持的片段来源类型：" + sourceType);
    }

    private String shotDisplayName(StoryboardShot shot) {
        String title = safe(shot == null ? null : shot.title).trim();
        if (!title.isBlank()) {
            return title;
        }
        return safe(shot == null ? null : shot.shotId);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int safeVersion(Integer version) {
        return version == null || version < 0 ? 0 : version;
    }

    private String nextId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
