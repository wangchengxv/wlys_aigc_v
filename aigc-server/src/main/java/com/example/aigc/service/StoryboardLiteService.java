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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StoryboardLiteService {
    private static final Logger log = LoggerFactory.getLogger(StoryboardLiteService.class);
    private static final String THREE_VIEW_EXTRACTION_TEMPLATE = "prompts/visual/three-view-extraction-user.md";

    private final GenerationService generationService;
    private final ImageGenerationCapabilityService imageGenerationCapabilityService;
    private final LocalAssetFileService localAssetFileService;
    private final TransactionTemplate transactionTemplate;
    private final AiCapabilityRoutingService aiCapabilityRoutingService;
    private final ProviderHttpGateway providerHttpGateway;
    private final PromptTemplateService promptTemplateService;
    @PersistenceContext
    private EntityManager entityManager;

    public StoryboardLiteService(
            GenerationService generationService,
            ImageGenerationCapabilityService imageGenerationCapabilityService,
            LocalAssetFileService localAssetFileService,
            PlatformTransactionManager transactionManager,
            AiCapabilityRoutingService aiCapabilityRoutingService,
            ProviderHttpGateway providerHttpGateway,
            PromptTemplateService promptTemplateService
    ) {
        this.generationService = generationService;
        this.imageGenerationCapabilityService = imageGenerationCapabilityService;
        this.localAssetFileService = localAssetFileService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.aiCapabilityRoutingService = aiCapabilityRoutingService;
        this.providerHttpGateway = providerHttpGateway;
        this.promptTemplateService = promptTemplateService;
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

    public List<StoryboardLiteDtos.KeyframeData> generateKeyframes(String ownerId, String sessionId, StoryboardLiteDtos.GenerateKeyframesRequest request) {
        try {
            LiteKeyframeGenerationInput input = transactionTemplate.execute(status -> {
                StoryboardLiteSession session = requireSession(ownerId, sessionId);
                StoryboardLiteScript script = requireLatestScript(sessionId);
                String prompt;
                if (request.prompt() == null || request.prompt().isBlank()) {
                    String extractedPrompt = extractThreeViewPromptWithLlm(script.scriptText);
                    prompt = extractedPrompt != null ? extractedPrompt
                        : "请基于下述剧本生成一张三视图（正面、侧面、背面）设定图，画面清晰，角色主体明确：\n"
                          + abbreviate(script.scriptText, 1200);
                } else {
                    prompt = request.prompt().trim();
                }
                return new LiteKeyframeGenerationInput(
                        prompt,
                        trimToNull(request.imageModel())
                );
            });
            if (input == null) {
                throw new BizException(500, "关键帧生成失败：事务未返回上下文");
            }

            ImageGenerationCapabilityService.ImageGenerationResult generationResult =
                    imageGenerationCapabilityService.generateImages(input.prompt(), 1, input.imageModel(), null, false);
            if (generationResult == null) {
                throw new BizException(500, "关键帧生成失败：服务未返回结果");
            }
            String rawImage = firstNonBlank(generationResult.results());
            if (rawImage == null) {
                throw new BizException(502, "图片模型未返回可用图片（模型：" + firstNonBlank(generationResult.modelName(), input.imageModel(), "自动路由") + "）");
            }

            return transactionTemplate.execute(status -> {
                StoryboardLiteSession session = requireSession(ownerId, sessionId);
                GeneratedLiteImage generatedImage = storeGeneratedLiteImage(session, rawImage, generationResult.modelName());
                Instant now = Instant.now();
                StoryboardLiteKeyframe keyframe = new StoryboardLiteKeyframe();
                keyframe.keyframeId = nextId("sbl-kf");
                keyframe.sessionId = sessionId;
                keyframe.promptText = input.prompt();
                keyframe.imageUrl = generatedImage.imageUrl();
                keyframe.imageFileId = generatedImage.imageFileId();
                keyframe.modelName = firstNonBlank(generatedImage.modelName(), input.imageModel());
                keyframe.selected = listKeyframes(sessionId).isEmpty();
                keyframe.status = "SUCCESS";
                keyframe.createdAt = now;
                keyframe.updatedAt = now;
                entityManager.persist(keyframe);
                session.status = "KEYFRAME_READY";
                session.updatedAt = now;
                entityManager.merge(session);
                return listKeyframes(sessionId).stream().map(this::toKeyframeData).toList();
            });
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

    public StoryboardLiteDtos.VideoTaskData generateVideo(String ownerId, String sessionId, StoryboardLiteDtos.GenerateVideoRequest request) {
        try {
            LiteVideoGenerationInput input = transactionTemplate.execute(status -> {
                requireSession(ownerId, sessionId);
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
                return new LiteVideoGenerationInput(
                        keyframe == null ? null : keyframe.keyframeId,
                        prompt,
                        style,
                        trimToNull(request.videoModel()),
                        directReference != null ? directReference : resolveVideoReferenceImage(keyframe)
                );
            });
            if (input == null) {
                throw new BizException(500, "图生视频生成失败：事务未返回上下文");
            }

            GenerateRequest generateRequest = new GenerateRequest(
                    input.prompt(),
                    GenerateMode.video,
                    input.style(),
                    "1024x1024",
                    "medium",
                    1,
                    null,
                    input.videoModel(),
                    null,
                    input.referenceImageUrl(),
                    null
            );
            GenerateResponseData result = generationService.generate(generateRequest, ownerId);
            if (result == null) {
                throw new BizException(500, "图生视频生成失败：服务未返回结果");
            }

            return transactionTemplate.execute(status -> {
                StoryboardLiteSession session = requireSession(ownerId, sessionId);
                String videoUrl = firstOrNull(result.videoResults());
                String videoFileId = firstOrNull(result.persistedVideoFileIds());
                Instant now = Instant.now();
                StoryboardLiteVideoTask task = new StoryboardLiteVideoTask();
                task.videoTaskId = nextId("sbl-vtask");
                task.sessionId = sessionId;
                task.keyframeId = input.keyframeId();
                task.promptText = input.prompt();
                task.providerTaskId = result.taskId();
                task.status = String.valueOf(result.status());
                task.videoUrl = videoUrl;
                task.resultVideoFileId = videoFileId;
                task.modelName = firstNonBlank(trimToNull(result.videoModel()), input.videoModel());
                task.errorMessage = null;
                task.createdAt = now;
                task.updatedAt = now;
                entityManager.persist(task);
                session.status = "VIDEO_READY";
                session.updatedAt = now;
                entityManager.merge(session);
                return toVideoTaskData(task);
            });
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

    private GeneratedLiteImage storeGeneratedLiteImage(StoryboardLiteSession session, String rawImage, String modelName) {
        String projectId = firstNonBlank(trimToNull(session.projectId), WorkspaceConstants.WORKSPACE_PROJECT_ID);
        String relativePath = "storyboard-lite/" + session.sessionId + "/" + nextId("keyframe") + ".png";
        String trimmed = rawImage.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                StoredFileRecord file = localAssetFileService.storeRemote(projectId, relativePath, "image/png", trimmed);
                entityManager.persist(file);
                return new GeneratedLiteImage(localAssetFileService.toPublicUrl(file.fileId), file.fileId, trimToNull(modelName));
            } catch (Exception ex) {
                log.warn("[StoryboardLite] 三视图远程图片落盘失败，保留模型外链继续流程: sessionId={}, error={}",
                        session.sessionId, rootMessage(ex));
                return new GeneratedLiteImage(trimmed, null, trimToNull(modelName));
            }
        }
        StoredFileRecord file = localAssetFileService.storeBase64(projectId, relativePath, "image/png", trimmed);
        entityManager.persist(file);
        return new GeneratedLiteImage(localAssetFileService.toPublicUrl(file.fileId), file.fileId, trimToNull(modelName));
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
            if (direct != null && direct.startsWith("/api/v1/files/")) {
                log.warn("[StoryboardLite] 关键帧参考图 fileId 不存在但路径有效，尝试从路径提取: direct={}", direct);
                fileId = trimToNull(direct.substring("/api/v1/files/".length()));
            }
            if (fileId == null) {
                return direct;
            }
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

    private String extractThreeViewPromptWithLlm(String scriptText) {
        try {
            AiCapabilityRoutingService.ResolvedAiModel resolved = aiCapabilityRoutingService.resolveText(null);
            if (resolved == null || !resolved.hasProvider()) {
                log.warn("[StoryboardLite] 未找到可用的文本模型，跳过 LLM 提示词增强");
                return null;
            }
            String template = promptTemplateService.load(THREE_VIEW_EXTRACTION_TEMPLATE, null);
            if (template == null) {
                log.warn("[StoryboardLite] 未找到三视图提取提示词模板，跳过 LLM 增强");
                return null;
            }
            String userPrompt = template.replace("{{scriptText}}", abbreviate(scriptText, 4000));
            Map<String, Object> payload;
            if ("anthropic".equalsIgnoreCase(resolved.provider().apiFormat())) {
                payload = Map.of(
                        "model", resolved.modelName(),
                        "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                        "max_tokens", 2000,
                        "temperature", 0.5
                );
            } else {
                payload = Map.of(
                        "model", resolved.modelName(),
                        "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                        "max_tokens", 2000,
                        "temperature", 0.5
                );
            }
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolved.provider(),
                    resolved.connection().getBaseUrl(),
                    resolved.apiKey(),
                    resolved.metadataPlain(),
                    payload,
                    Duration.ofSeconds(30)
            );
            String content = parseAssistantContent(response, resolved.provider().apiFormat());
            if (content == null || content.isBlank()) {
                log.warn("[StoryboardLite] LLM 返回内容为空，跳过提示词增强");
                return null;
            }
            log.info("[StoryboardLite] LLM 提示词增强成功，长度: {}", content.length());
            return content.trim();
        } catch (Exception ex) {
            log.warn("[StoryboardLite] LLM 提示词增强失败，回退到简单模板: {}", rootMessage(ex));
            return null;
        }
    }

    private String parseAssistantContent(Map<String, Object> response, String apiFormat) {
        if ("anthropic".equalsIgnoreCase(apiFormat)) {
            Object contentNode = response.get("content");
            if (contentNode instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                Object text = first.get("text");
                return text == null ? "" : String.valueOf(text);
            }
            return "";
        }
        Object choicesNode = response.get("choices");
        if (choicesNode instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object messageNode = first.get("message");
            if (messageNode instanceof Map<?, ?> message) {
                Object content = message.get("content");
                return content == null ? "" : String.valueOf(content);
            }
        }
        return "";
    }

    private String firstNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
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

    private record LiteKeyframeGenerationInput(String prompt, String imageModel) {
    }

    private record LiteVideoGenerationInput(String keyframeId, String prompt, String style, String videoModel, String referenceImageUrl) {
    }
}
