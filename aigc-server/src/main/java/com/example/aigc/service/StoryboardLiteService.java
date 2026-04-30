package com.example.aigc.service;

import com.example.aigc.constants.WorkspaceConstants;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.StoryboardLiteDtos;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardLiteKeyframe;
import com.example.aigc.entity.StoryboardLiteScript;
import com.example.aigc.entity.StoryboardLiteSession;
import com.example.aigc.entity.StoryboardLiteVideoTask;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.exception.BizException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class StoryboardLiteService {
    private final GenerationService generationService;
    private final LocalAssetFileService localAssetFileService;
    @PersistenceContext
    private EntityManager entityManager;

    public StoryboardLiteService(GenerationService generationService, LocalAssetFileService localAssetFileService) {
        this.generationService = generationService;
        this.localAssetFileService = localAssetFileService;
    }

    @Transactional
    public StoryboardLiteDtos.SessionData createSession(String ownerId, StoryboardLiteDtos.CreateSessionRequest request) {
        Instant now = Instant.now();
        StoryboardLiteSession session = new StoryboardLiteSession();
        session.sessionId = nextId("sbl");
        session.ownerId = ownerId;
        session.projectId = trimToNull(request.projectId());
        session.title = trimToNull(request.title());
        session.status = "DRAFT";
        session.createdAt = now;
        session.updatedAt = now;
        entityManager.persist(session);
        return toSessionData(session);
    }

    @Transactional
    public StoryboardLiteDtos.SessionData saveScript(String ownerId, String sessionId, StoryboardLiteDtos.SaveScriptRequest request) {
        StoryboardLiteSession session = requireSession(ownerId, sessionId);
        int nextVersion = listScripts(sessionId).stream().map(item -> item.versionNo).max(Comparator.naturalOrder()).orElse(0) + 1;
        StoryboardLiteScript script = new StoryboardLiteScript();
        script.scriptId = nextId("sbl-script");
        script.sessionId = sessionId;
        script.scriptText = request.scriptText().trim();
        script.versionNo = nextVersion;
        script.createdAt = Instant.now();
        entityManager.persist(script);
        session.status = "SCRIPT_READY";
        session.updatedAt = Instant.now();
        entityManager.merge(session);
        return toSessionData(session);
    }

    @Transactional
    public List<StoryboardLiteDtos.KeyframeData> generateKeyframes(String ownerId, String sessionId, StoryboardLiteDtos.GenerateKeyframesRequest request) {
        try {
            StoryboardLiteSession session = requireSession(ownerId, sessionId);
            StoryboardLiteScript script = requireLatestScript(sessionId);
            String style = request.style() == null || request.style().isBlank() ? "影视级真实" : request.style().trim();
            String prompt = request.prompt() == null || request.prompt().isBlank()
                    ? "请基于下述剧本生成一张三视图（正面、侧面、背面）设定图，画面清晰，角色主体明确：\n"
                    + abbreviate(script.scriptText, 1200)
                    : request.prompt().trim();
            GenerateRequest generateRequest = new GenerateRequest(
                    prompt,
                    GenerateMode.image,
                    style,
                    "1024x1024",
                    "medium",
                    1,
                    trimToNull(request.imageModel()),
                    null,
                    null,
                    null,
                    null
            );
            GeneratedLiteImage generatedImage = generateLiteImage(session, ownerId, generateRequest);
            Instant now = Instant.now();
            StoryboardLiteKeyframe keyframe = new StoryboardLiteKeyframe();
            keyframe.keyframeId = nextId("sbl-kf");
            keyframe.sessionId = sessionId;
            keyframe.promptText = prompt;
            keyframe.imageUrl = generatedImage.imageUrl();
            keyframe.imageFileId = generatedImage.imageFileId();
            keyframe.modelName = firstNonBlank(generatedImage.modelName(), trimToNull(request.imageModel()));
            keyframe.selected = listKeyframes(sessionId).isEmpty();
            keyframe.status = "SUCCESS";
            keyframe.createdAt = now;
            keyframe.updatedAt = now;
            entityManager.persist(keyframe);
            session.status = "KEYFRAME_READY";
            session.updatedAt = now;
            entityManager.merge(session);
            return listKeyframes(sessionId).stream().map(this::toKeyframeData).toList();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(500, "关键帧生成失败：" + rootMessage(ex));
        }
    }

    @Transactional
    public StoryboardLiteDtos.ConfirmKeyframeResponse confirmKeyframe(String ownerId, String sessionId, String keyframeId) {
        requireSession(ownerId, sessionId);
        List<StoryboardLiteKeyframe> keyframes = listKeyframes(sessionId);
        StoryboardLiteKeyframe target = keyframes.stream()
                .filter(item -> Objects.equals(item.keyframeId, keyframeId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "关键帧不存在"));
        Instant now = Instant.now();
        for (StoryboardLiteKeyframe keyframe : keyframes) {
            keyframe.selected = Objects.equals(keyframe.keyframeId, keyframeId);
            keyframe.updatedAt = now;
            entityManager.merge(keyframe);
        }
        return new StoryboardLiteDtos.ConfirmKeyframeResponse(target.keyframeId, true);
    }

    @Transactional
    public StoryboardLiteDtos.VideoTaskData generateVideo(String ownerId, String sessionId, StoryboardLiteDtos.GenerateVideoRequest request) {
        try {
            StoryboardLiteSession session = requireSession(ownerId, sessionId);
            String directReference = trimToNull(request.referenceImageUrl());
            StoryboardLiteKeyframe keyframe = null;
            if (directReference == null) {
                String keyframeId = trimToNull(request.keyframeId());
                if (keyframeId == null) {
                    throw new BizException(400, "请先确认关键帧，或上传/粘贴一张首帧图片");
                }
                keyframe = listKeyframes(sessionId).stream()
                        .filter(item -> Objects.equals(item.keyframeId, keyframeId))
                        .findFirst()
                        .orElseThrow(() -> new BizException(404, "关键帧不存在"));
                if (!keyframe.selected) {
                    throw new BizException(400, "请先确认关键帧后再生成视频");
                }
            }
            String style = request.style() == null || request.style().isBlank() ? "影视级真实" : request.style().trim();
            String prompt = request.prompt() == null || request.prompt().isBlank() ? "请基于参考图生成5秒电影感镜头。" : request.prompt().trim();
            GenerateRequest generateRequest = new GenerateRequest(
                    prompt,
                    GenerateMode.video,
                    style,
                    "1024x1024",
                    "medium",
                    1,
                    null,
                    trimToNull(request.videoModel()),
                    null,
                    directReference != null ? directReference : resolveVideoReferenceImage(keyframe),
                    null
            );
            GenerateResponseData result = generationService.generate(generateRequest, ownerId);
            if (result == null) {
                throw new BizException(500, "图生视频生成失败：服务未返回结果");
            }
            String videoUrl = firstOrNull(result.videoResults());
            String videoFileId = firstOrNull(result.persistedVideoFileIds());
            Instant now = Instant.now();
            StoryboardLiteVideoTask task = new StoryboardLiteVideoTask();
            task.videoTaskId = nextId("sbl-vtask");
            task.sessionId = sessionId;
            task.keyframeId = keyframe == null ? null : keyframe.keyframeId;
            task.promptText = prompt;
            task.providerTaskId = result.taskId();
            task.status = String.valueOf(result.status());
            task.videoUrl = videoUrl;
            task.resultVideoFileId = videoFileId;
            task.modelName = firstNonBlank(trimToNull(result.videoModel()), trimToNull(request.videoModel()));
            task.errorMessage = null;
            task.createdAt = now;
            task.updatedAt = now;
            entityManager.persist(task);
            session.status = "VIDEO_READY";
            session.updatedAt = now;
            entityManager.merge(session);
            return toVideoTaskData(task);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(500, "图生视频生成失败：" + rootMessage(ex));
        }
    }

    @Transactional
    public StoryboardLiteDtos.SessionData getSession(String ownerId, String sessionId) {
        StoryboardLiteSession session = requireSession(ownerId, sessionId);
        return toSessionData(session);
    }

    private StoryboardLiteSession requireSession(String ownerId, String sessionId) {
        StoryboardLiteSession session = entityManager.find(StoryboardLiteSession.class, sessionId);
        if (session == null || !Objects.equals(session.ownerId, ownerId)) {
            throw new BizException(404, "会话不存在");
        }
        return session;
    }

    private StoryboardLiteScript requireLatestScript(String sessionId) {
        return listScripts(sessionId).stream().max(Comparator.comparingInt(item -> item.versionNo))
                .orElseThrow(() -> new BizException(400, "请先保存剧本"));
    }

    private List<StoryboardLiteScript> listScripts(String sessionId) {
        return entityManager.createQuery(
                        "select s from StoryboardLiteScript s where s.sessionId = :sessionId order by s.versionNo desc",
                        StoryboardLiteScript.class)
                .setParameter("sessionId", sessionId)
                .getResultList();
    }

    private List<StoryboardLiteKeyframe> listKeyframes(String sessionId) {
        return entityManager.createQuery(
                        "select k from StoryboardLiteKeyframe k where k.sessionId = :sessionId order by k.createdAt asc",
                        StoryboardLiteKeyframe.class)
                .setParameter("sessionId", sessionId)
                .getResultList();
    }

    private List<StoryboardLiteVideoTask> listVideoTasks(String sessionId) {
        return entityManager.createQuery(
                        "select v from StoryboardLiteVideoTask v where v.sessionId = :sessionId order by v.createdAt desc",
                        StoryboardLiteVideoTask.class)
                .setParameter("sessionId", sessionId)
                .getResultList();
    }

    private StoryboardLiteDtos.SessionData toSessionData(StoryboardLiteSession session) {
        String latestScript = listScripts(session.sessionId).stream().findFirst().map(item -> item.scriptText).orElse(null);
        List<StoryboardLiteDtos.KeyframeData> keyframes = listKeyframes(session.sessionId).stream().map(this::toKeyframeData).toList();
        List<StoryboardLiteDtos.VideoTaskData> videoTasks = listVideoTasks(session.sessionId).stream().map(this::toVideoTaskData).toList();
        return new StoryboardLiteDtos.SessionData(
                session.sessionId,
                session.ownerId,
                session.projectId,
                session.title,
                session.status,
                latestScript,
                keyframes,
                videoTasks
        );
    }

    private StoryboardLiteDtos.KeyframeData toKeyframeData(StoryboardLiteKeyframe keyframe) {
        return new StoryboardLiteDtos.KeyframeData(
                keyframe.keyframeId,
                keyframe.promptText,
                keyframe.imageUrl,
                keyframe.imageFileId,
                keyframe.modelName,
                keyframe.selected,
                keyframe.status,
                keyframe.createdAt == null ? null : keyframe.createdAt.toString()
        );
    }

    private StoryboardLiteDtos.VideoTaskData toVideoTaskData(StoryboardLiteVideoTask task) {
        return new StoryboardLiteDtos.VideoTaskData(
                task.videoTaskId,
                task.keyframeId,
                task.status,
                task.videoUrl,
                task.resultVideoFileId,
                task.modelName,
                task.errorMessage,
                task.createdAt == null ? null : task.createdAt.toString()
        );
    }

    private String nextId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String abbreviate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private String firstOrNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.get(0);
        return value == null || value.isBlank() ? null : value;
    }

    private GeneratedLiteImage generateLiteImage(StoryboardLiteSession session, String ownerId, GenerateRequest request) {
        GenerateResponseData result = generationService.generate(request, ownerId);
        if (result == null) {
            throw new BizException(500, "关键帧生成失败：服务未返回结果");
        }

        String imageUrl = firstOrNull(result.imageResults());
        String imageFileId = firstOrNull(result.persistedImageFileIds());
        if (imageFileId != null) {
            return new GeneratedLiteImage(
                    firstNonBlank(localAssetFileService.toPublicUrl(imageFileId), imageUrl),
                    imageFileId,
                    trimToNull(result.imageModel())
            );
        }
        if (imageUrl == null) {
            throw new BizException(500, "关键帧生成失败：未返回图片");
        }

        StoredFileRecord stored = storeGeneratedLiteImage(session, imageUrl);
        return new GeneratedLiteImage(localAssetFileService.toPublicUrl(stored.fileId), stored.fileId, trimToNull(result.imageModel()));
    }

    private StoredFileRecord storeGeneratedLiteImage(StoryboardLiteSession session, String rawImage) {
        String projectId = firstNonBlank(trimToNull(session.projectId), WorkspaceConstants.WORKSPACE_PROJECT_ID);
        String relativePath = "storyboard-lite/" + session.sessionId + "/" + nextId("keyframe") + ".png";
        String trimmed = rawImage.trim();
        StoredFileRecord file = trimmed.startsWith("http://") || trimmed.startsWith("https://")
                ? localAssetFileService.storeRemote(projectId, relativePath, "image/png", trimmed)
                : localAssetFileService.storeBase64(projectId, relativePath, "image/png", trimmed);
        entityManager.persist(file);
        return file;
    }

    private String resolveVideoReferenceImage(StoryboardLiteKeyframe keyframe) {
        if (keyframe == null) {
            return null;
        }
        String direct = trimToNull(keyframe.imageUrl);
        if (direct != null && (direct.startsWith("http://") || direct.startsWith("https://") || direct.startsWith("data:image/"))) {
            return direct;
        }
        String fileId = trimToNull(keyframe.imageFileId);
        if (fileId == null && direct != null && direct.startsWith("/api/v1/files/")) {
            fileId = trimToNull(direct.substring("/api/v1/files/".length()));
        }
        if (fileId == null) {
            return direct;
        }
        StoredFileRecord record = entityManager.find(StoredFileRecord.class, fileId);
        if (record == null || !localAssetFileService.exists(record)) {
            return direct;
        }
        String mediaType = firstNonBlank(trimToNull(record.mediaType), "image/png");
        byte[] bytes = localAssetFileService.readBytes(record);
        return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    private record GeneratedLiteImage(String imageUrl, String imageFileId, String modelName) {
    }
}
