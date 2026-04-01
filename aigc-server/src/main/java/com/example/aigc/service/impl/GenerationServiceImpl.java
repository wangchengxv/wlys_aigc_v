package com.example.aigc.service.impl;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.entity.GenerationTask;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.enums.TaskStatus;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.GenerationTaskRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.service.ApiKeyCryptoService;
import com.example.aigc.service.GenerationService;
import com.example.aigc.service.ModelCapabilityService;
import com.example.aigc.service.GatewayKind;
import com.example.aigc.service.ProviderCatalog;
import com.example.aigc.service.ProviderGatewayException;
import com.example.aigc.service.ProviderHttpGateway;
import com.example.aigc.service.RouterRoutingService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;

@Service
public class GenerationServiceImpl implements GenerationService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<String> BLOCK_WORDS = List.of("暴恐", "色情", "违禁", "涉政");
    /** 视频：全局风格 + 用户描述合并后最大长度（避免超出模型侧限制） */
    private static final int MAX_VIDEO_MERGED_PROMPT_CHARS = 8000;

    private final GenerationTaskRepository repository;
    private final AigcArkProperties arkProperties;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final ProviderCatalog providerCatalog;
    private final ProviderHttpGateway providerHttpGateway;
    private final ModelCapabilityService modelCapabilityService;
    private final RouterRoutingService routerRoutingService;
    private final VideoStylePresetRegistry videoStylePresetRegistry;

    public GenerationServiceImpl(
            GenerationTaskRepository repository,
            AigcArkProperties arkProperties,
            ConnectionConfigRepository connectionConfigRepository,
            ModelConfigRepository modelConfigRepository,
            ApiKeyCryptoService apiKeyCryptoService,
            ProviderCatalog providerCatalog,
            ProviderHttpGateway providerHttpGateway,
            ModelCapabilityService modelCapabilityService,
            RouterRoutingService routerRoutingService,
            VideoStylePresetRegistry videoStylePresetRegistry
    ) {
        this.repository = repository;
        this.arkProperties = arkProperties;
        this.connectionConfigRepository = connectionConfigRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.providerCatalog = providerCatalog;
        this.providerHttpGateway = providerHttpGateway;
        this.modelCapabilityService = modelCapabilityService;
        this.routerRoutingService = routerRoutingService;
        this.videoStylePresetRegistry = videoStylePresetRegistry;
    }

    @Override
    public GenerateResponseData generate(GenerateRequest request, String ownerId) {
        validatePrompt(request.prompt());
        String styleKey = videoStylePresetRegistry.normalizeStyleForWrite(request.style());
        validatePrompt(styleKey);
        if (ownerId == null || ownerId.isBlank()) {
            throw new BizException(401, "缺少用户标识");
        }

        long start = System.currentTimeMillis();
        String taskId = "T" + UUID.randomUUID().toString().replace("-", "");
        GenerationTask task = new GenerationTask();
        task.setTaskId(taskId);
        task.setOwnerId(ownerId);
        task.setPrompt(request.prompt());
        task.setMode(request.mode());
        task.setStyle(styleKey);
        task.setCreatedAt(LocalDateTime.now());
        task.setStatus(TaskStatus.PROCESSING);
        repository.save(task);

        List<String> textResults = new ArrayList<>();
        List<String> imageResults = new ArrayList<>();
        List<String> videoResults = new ArrayList<>();
        try {
            if (request.mode() == GenerateMode.text || request.mode() == GenerateMode.both) {
                textResults = generateTextContent(request.prompt(), styleKey, request.textLength());
            }
            if (request.mode() == GenerateMode.image || request.mode() == GenerateMode.both) {
                MediaResult imageResult = generateImages(request.prompt(), safeCount(request.count()), request.imageModel());
                imageResults = imageResult.results();
                task.setImageModel(imageResult.modelName());
                task.setImageModelSource(imageResult.modelSource());
                task.setImageModelMatchedBy(imageResult.matchedBy());
                task.setImageModelRejectReason(imageResult.rejectReason());
            }
            if (request.mode() == GenerateMode.video) {
                String videoPrompt = buildVideoPrompt(styleKey, request.prompt());
                validateVideoMergedPromptLength(videoPrompt);
                MediaResult videoResult = generateVideos(videoPrompt, safeCount(request.count()), request.videoModel(), request.videoReferenceImageUrl());
                videoResults = videoResult.results();
                task.setVideoModel(videoResult.modelName());
                task.setVideoModelSource(videoResult.modelSource());
                task.setVideoModelMatchedBy(videoResult.matchedBy());
                task.setVideoModelRejectReason(videoResult.rejectReason());
            }
            task.setTextResults(textResults);
            task.setImageResults(imageResults);
            task.setVideoResults(videoResults);
            task.setStatus(TaskStatus.SUCCESS);
            task.setErrorCode(null);
            return toData(task);
        } catch (BizException ex) {
            task.setStatus(TaskStatus.FAIL);
            task.setErrorCode("BIZ_" + ex.getStatus());
            throw ex;
        } catch (Exception ex) {
            task.setStatus(TaskStatus.FAIL);
            task.setErrorCode("INTERNAL_ERROR");
            throw ex;
        } finally {
            task.setLatencyMs(System.currentTimeMillis() - start);
            repository.save(task);
        }
    }

    @Override
    public PagedResult<GenerateResponseData> history(int page, int pageSize, GenerateMode mode, String ownerId) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(Math.min(pageSize, 50), 1);
        List<GenerateResponseData> list = repository.page(safePage, safePageSize, mode, ownerId).stream().map(this::toData).toList();
        return new PagedResult<>(list, repository.count(mode, ownerId));
    }

    @Override
    public GenerateResponseData taskDetail(String taskId, String ownerId) {
        GenerationTask task = requireOwnedTask(taskId, ownerId);
        return toData(task);
    }

    @Override
    public void deleteTask(String taskId, String ownerId) {
        GenerationTask task = repository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            return;
        }
        if (!ownerId.equals(task.getOwnerId())) {
            throw new BizException(403, "无权删除该任务");
        }
        repository.deleteByTaskId(taskId);
    }

    private void validatePrompt(String prompt) {
        for (String word : BLOCK_WORDS) {
            if (prompt.contains(word)) {
                throw new BizException(400, "输入内容包含敏感词，请调整后重试");
            }
        }
    }

    /**
     * 视频：风格约束在前，用户描述在后（与前端「全局设定优先」一致）。
     */
    private String buildVideoPrompt(String styleKey, String prompt) {
        String s = "Visual Style Anchor: " + videoStylePresetRegistry.resolveAnchorByStyleKey(styleKey);
        String p = prompt == null ? "" : prompt.trim();
        String n = "Negative Prompt: " + videoStylePresetRegistry.videoNegativePromptEnglish();
        if (s.isEmpty()) {
            return p;
        }
        if (p.isEmpty()) {
            return s + "\n" + n;
        }
        return s + "\n" + p + "\n" + n;
    }

    private void validateVideoMergedPromptLength(String merged) {
        if (merged.length() > MAX_VIDEO_MERGED_PROMPT_CHARS) {
            throw new BizException(400, "视频提示词过长（含全局风格与用户描述），请缩短后重试");
        }
    }

    private int safeCount(Integer count) {
        if (count == null) {
            return 1;
        }
        return Math.max(1, Math.min(count, 4));
    }

    private List<String> generateTextContent(String prompt, String style, String textLength) {
        ResolvedModel resolvedModel = resolveModel("text", null);
        if (resolvedModel == null) {
            throw new BizException(400, "未配置可用的文本模型");
        }

        Map<String, Object> requestPayload = buildTextRequest(prompt, style, textLength, resolvedModel);
        try {
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    requestPayload,
                    Duration.ofSeconds(Math.max(8, routerRoutingService.timeoutSeconds()))
            );
            String content = parseAssistantContent(response, resolvedModel.provider().apiFormat());
            List<String> lines = splitTextResults(content);
            if (lines.isEmpty()) {
                throw new BizException(502, "文本模型返回为空");
            }
            return lines;
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private MediaResult generateImages(String prompt, int count, String requestedModel) {
        ResolvedModel resolvedModel = resolveModel("image", requestedModel);
        if (resolvedModel != null) {
            return new MediaResult(
                    resolvedModel.model().getModelName(),
                    generateImagesWithConfiguredModel(prompt, count, resolvedModel),
                    resolvedModel.source(),
                    resolvedModel.matchedBy(),
                    resolvedModel.rejectReason()
            );
        }
        if (requestedModel != null && !requestedModel.isBlank()) {
            throw new BizException(400, "图片模型未在可用配置中");
        }
        String selectedModel = arkProperties.getDefaultImageModel();
        return new MediaResult(
                selectedModel,
                generateImagesFromArk(prompt, count, selectedModel),
                "SYSTEM_FALLBACK",
                "default-image",
                null
        );
    }

    private MediaResult generateVideos(String prompt, int count, String requestedModel, String videoReferenceImageUrl) {
        ResolvedModel resolvedModel = resolveModel("video", requestedModel);
        if (resolvedModel != null) {
            if ("ark".equalsIgnoreCase(resolvedModel.provider().key())) {
                return new MediaResult(
                        resolvedModel.model().getModelName(),
                        generateVideosWithArkConnection(prompt, count, resolvedModel),
                        resolvedModel.source(),
                        resolvedModel.matchedBy(),
                        resolvedModel.rejectReason()
                );
            }
            if ("moark".equalsIgnoreCase(resolvedModel.provider().key())) {
                return new MediaResult(
                        resolvedModel.model().getModelName(),
                        generateVideosWithMoarkConnection(prompt, count, resolvedModel, videoReferenceImageUrl),
                        resolvedModel.source(),
                        resolvedModel.matchedBy(),
                        resolvedModel.rejectReason()
                );
            }
            throw new BizException(400, "当前视频模型仅支持配置为方舟(ark)或 Moark(moark) 连接");
        }
        if (requestedModel != null && !requestedModel.isBlank()) {
            throw new BizException(400, "视频模型未在可用配置中");
        }
        String selectedModel = arkProperties.getDefaultVideoModel();
        return new MediaResult(
                selectedModel,
                generateVideosFromArk(prompt, count, selectedModel),
                "SYSTEM_FALLBACK",
                "default-video",
                null
        );
    }

    private GenerationTask requireOwnedTask(String taskId, String ownerId) {
        GenerationTask task = repository.findByTaskId(taskId).orElseThrow(() -> new BizException(404, "任务不存在"));
        if (!ownerId.equals(task.getOwnerId())) {
            throw new BizException(403, "无权访问该任务");
        }
        return task;
    }

    private Map<String, Object> buildTextRequest(String prompt, String style, String textLength, ResolvedModel resolvedModel) {
        String lengthDesc = switch (textLength == null ? "medium" : textLength) {
            case "short" -> "短一点，适合海报或 banner";
            case "long" -> "长一点，适合详情页或社媒长文案";
            default -> "中等长度，适合朋友圈、公众号摘要或落地页";
        };
        String systemPrompt = "你是一名中文营销创意助手，擅长产出可直接使用的高质量文案。";
        String userPrompt = """
                请围绕以下主题生成 2 条中文营销文案，要求：
                1. 风格为：%s
                2. 长度要求：%s
                3. 每条文案都要完整、可直接发布
                4. 请不要输出解释，只输出两条文案，每条单独一行

                主题：%s
                """.formatted(style, lengthDesc, prompt);

        if ("anthropic".equalsIgnoreCase(resolvedModel.provider().apiFormat())) {
            return Map.of(
                    "model", resolvedModel.model().getModelName(),
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                    "max_tokens", 1000,
                    "temperature", 0.8
            );
        }

        return Map.of(
                "model", resolvedModel.model().getModelName(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.8,
                "max_tokens", 1000
        );
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

    private List<String> splitTextResults(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> lines = content.lines()
                .map(String::trim)
                .map(this::stripLeadingBullet)
                .filter(line -> !line.isBlank())
                .distinct()
                .limit(4)
                .toList();
        if (lines.isEmpty()) {
            return List.of(content.trim());
        }
        return lines;
    }

    private String stripLeadingBullet(String line) {
        return line.replaceFirst("^[\\-•\\d\\.\\)]\\s*", "").trim();
    }

    private List<String> generateImagesWithConfiguredModel(String prompt, int count, ResolvedModel resolvedModel) {
        ProviderCatalog.ProviderDefinition provider = resolvedModel.provider();
        if (provider.imageGenerationPath() == null || provider.imageGenerationPath().isBlank()) {
            throw new BizException(400, "当前图片模型对应连接未配置图片生成接口");
        }
        List<String> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> body;
            if ("ark".equalsIgnoreCase(provider.key())) {
                body = callArkImageApi(resolvedModel.connection().getBaseUrl(), resolvedModel.apiKey(), prompt, resolvedModel.model().getModelName());
            } else {
                body = providerHttpGateway.generateImage(
                        provider,
                        resolvedModel.connection().getBaseUrl(),
                        resolvedModel.apiKey(),
                        Map.of(
                                "model", resolvedModel.model().getModelName(),
                                "prompt", prompt,
                                "n", 1,
                                "size", requestImageSize(),
                                "response_format", "url"
                        ),
                        Duration.ofSeconds(60)
                );
            }
            String imageUrl = parseImageUrl(body);
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new BizException(502, "模型服务返回异常，未获取到图片地址");
            }
            images.add(imageUrl);
        }
        return images;
    }

    private List<String> generateImagesFromArk(String prompt, int count, String imageModel) {
        String apiKey = requireArkApiKey();
        List<String> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> body = callArkImageApi(arkProperties.getBaseUrl(), apiKey, prompt, imageModel);
            String imageUrl = parseImageUrl(body);
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new BizException(502, "模型服务返回异常，未获取到图片地址");
            }
            images.add(imageUrl);
        }
        return images;
    }

    private Map<String, Object> callArkImageApi(String baseUrl, String apiKey, String prompt, String imageModel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", imageModel);
        payload.put("prompt", prompt);
        payload.put("sequential_image_generation", arkProperties.getSequentialImageGeneration());
        payload.put("response_format", arkProperties.getResponseFormat());
        payload.put("size", arkProperties.getSize());
        payload.put("stream", arkProperties.isStream());
        payload.put("watermark", arkProperties.isWatermark());
        try {
            return providerHttpGateway.generateImage(
                    providerCatalog.require("ark"),
                    baseUrl,
                    apiKey,
                    payload,
                    Duration.ofSeconds(60)
            );
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private List<String> generateVideosWithArkConnection(String prompt, int count, ResolvedModel resolvedModel) {
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String videoUrl = callArkVideoApi(
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    prompt,
                    resolvedModel.model().getModelName()
            );
            videos.add(videoUrl);
        }
        return videos;
    }

    private List<String> generateVideosWithMoarkConnection(String prompt, int count, ResolvedModel resolvedModel, String referenceImageUrl) {
        if (referenceImageUrl == null || referenceImageUrl.isBlank()) {
            throw new BizException(400, "Moark 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址）");
        }
        String trimmedUrl = referenceImageUrl.trim();
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            videos.add(callMoarkVideoApi(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    prompt,
                    resolvedModel.model().getModelName(),
                    trimmedUrl
            ));
        }
        return videos;
    }

    private List<String> generateVideosFromArk(String prompt, int count, String videoModel) {
        String apiKey = requireArkApiKey();
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String videoUrl = callArkVideoApi(arkProperties.getBaseUrl(), apiKey, prompt, videoModel);
            if (videoUrl == null || videoUrl.isBlank()) {
                throw new BizException(502, "模型服务返回异常，未获取到视频地址");
            }
            videos.add(videoUrl);
        }
        return videos;
    }

    private String callArkVideoApi(String baseUrl, String apiKey, String prompt, String videoModel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", videoModel);
        int duration = safeVideoDuration();
        boolean watermark = arkProperties.isWatermark();
        String textWithParams = prompt.trim()
                + " --duration " + duration
                + " --camerafixed false --watermark " + watermark;
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", textWithParams);
        payload.put("content", List.of(textContent));

        try {
            Map<String, Object> submitBody = providerHttpGateway.submitVideoTask(
                    providerCatalog.require("ark"),
                    baseUrl,
                    apiKey,
                    payload,
                    Duration.ofSeconds(60)
            );
            String directUrl = parseArkVideoUrl(submitBody, false);
            if (directUrl != null) {
                return directUrl;
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "视频模型服务返回异常，缺少task_id或视频地址");
            }
            return pollArkVideoTask(providerCatalog.require("ark"), baseUrl, apiKey, taskId);
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private String callMoarkVideoApi(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            String prompt,
            String videoModel,
            String imageUrl
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", prompt);
        payload.put("model", videoModel);
        payload.put("num_inference_steps", 50);
        payload.put("num_frames", 81);
        payload.put("image", imageUrl);
        try {
            Map<String, Object> submitBody = providerHttpGateway.submitVideoTask(
                    provider,
                    baseUrl,
                    apiKey,
                    connectionMetadata,
                    payload,
                    Duration.ofSeconds(120)
            );
            String directUrl = parseArkVideoUrl(submitBody, false);
            if (directUrl != null) {
                return directUrl;
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "Moark 未返回 task_id 或视频地址");
            }
            return pollArkVideoTask(provider, baseUrl, apiKey, taskId);
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private int safeVideoDuration() {
        Integer raw = arkProperties.getVideoDurationSeconds();
        if (raw == null) {
            return 5;
        }
        return Math.max(1, Math.min(raw, 30));
    }

    private String pollArkVideoTask(ProviderCatalog.ProviderDefinition provider, String baseUrl, String apiKey, String taskId) {
        int maxAttempts = Math.max(1, arkProperties.getVideoPollMaxAttempts());
        long intervalMs = Math.max(300L, arkProperties.getVideoPollIntervalMs());
        String lastStatus = null;
        Map<String, Object> lastBody = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> resultBody = requestVideoTaskResult(provider, baseUrl, apiKey, taskId);
            lastBody = resultBody;
            Object errNode = resultBody.get("error");
            if (errNode != null && !Boolean.FALSE.equals(errNode) && !"false".equalsIgnoreCase(String.valueOf(errNode).trim())) {
                throw new BizException(502, parseArkTaskError(resultBody));
            }
            String maybeUrl = parseArkVideoUrl(resultBody, false);
            if (maybeUrl != null) {
                return maybeUrl;
            }

            String status = parseArkTaskStatus(resultBody);
            lastStatus = status;
            if (isSuccessStatus(status) && attempt == maxAttempts) {
                throw new BizException(502, "视频任务已完成，但未返回视频地址，task_id="
                        + taskId + "，status=" + safeStatus(status) + "，响应摘要=" + summarizeBody(resultBody));
            }
            if (isFailedStatus(status)) {
                throw new BizException(502, "视频任务失败：" + parseArkTaskError(resultBody));
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new BizException(504, "视频任务轮询被中断，请稍后重试");
                }
            }
        }
        if (isSuccessStatus(lastStatus)) {
            throw new BizException(502, "视频任务已完成，但未返回视频地址，task_id="
                    + taskId + "，status=" + safeStatus(lastStatus) + "，响应摘要=" + summarizeBody(lastBody));
        }
        throw new BizException(504, "视频生成超时，请稍后重试或缩短提示词");
    }

    private Map<String, Object> requestVideoTaskResult(ProviderCatalog.ProviderDefinition provider, String baseUrl, String apiKey, String taskId) {
        try {
            return providerHttpGateway.queryVideoTask(
                    provider,
                    baseUrl,
                    apiKey,
                    taskId,
                    Duration.ofSeconds(30)
            );
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private String parseImageUrl(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object dataNode = body.get("data");
        if (!(dataNode instanceof List<?> dataList) || dataList.isEmpty()) {
            return null;
        }
        Object first = dataList.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object url = firstMap.get("url");
        if (url != null) {
            return String.valueOf(url);
        }
        Object b64 = firstMap.get("b64_json");
        return b64 == null ? null : String.valueOf(b64);
    }

    private ResolvedModel resolveModel(String capability, String requestedModel) {
        List<ModelConfig> candidates = modelConfigRepository.findAll().stream()
                .filter(ModelConfig::isEnabled)
                .filter(model -> modelCapabilityService.supports(model, capability))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        if (requestedModel != null && !requestedModel.isBlank()) {
            for (ModelConfig candidate : candidates) {
                String matchedBy = matchExplicitModel(candidate, requestedModel.trim());
                if (matchedBy != null) {
                    ResolvedModel resolved = toResolvedModel(candidate);
                    if (resolved != null) {
                        return resolved.withMatch(matchedBy);
                    }
                }
            }
            return null;
        }

        List<String> orderedConnectionIds = routerRoutingService.resolveOrderedConnections(true).stream()
                .map(ConnectionConfig::getId)
                .toList();
        for (String connectionId : orderedConnectionIds) {
            for (ModelConfig candidate : candidates) {
                if (connectionId.equals(candidate.getConnectionId())) {
                    ResolvedModel resolved = toResolvedModel(candidate);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }
        for (ModelConfig candidate : candidates) {
            ResolvedModel resolved = toResolvedModel(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private ResolvedModel toResolvedModel(ModelConfig model) {
        ConnectionConfig connection = connectionConfigRepository.findById(model.getConnectionId()).orElse(null);
        if (connection == null || !connection.isEnabled()) {
            return null;
        }
        ProviderCatalog.ProviderDefinition provider = providerCatalog.require(connection.getProvider());
        String apiKey = resolveApiKeyForGeneration(provider, connection);
        Map<String, Object> metaPlain = com.example.aigc.service.ConnectionMetadataHelper.decryptForUse(connection.getMetadata(), apiKeyCryptoService);
        return new ResolvedModel(model, connection, provider, apiKey, metaPlain, "USER_CONFIGURED", "modelName", null);
    }

    private String resolveApiKeyForGeneration(ProviderCatalog.ProviderDefinition provider, ConnectionConfig connection) {
        if (provider.gatewayKind() == GatewayKind.BEDROCK) {
            return decryptRequiredApiKey(connection);
        }
        if (provider.gatewayKind() == GatewayKind.VERTEX) {
            return "";
        }
        return provider.authMode() == ProviderCatalog.AuthMode.NONE ? "" : decryptRequiredApiKey(connection);
    }

    private String matchExplicitModel(ModelConfig modelConfig, String requestedModel) {
        String expected = normalize(requestedModel);
        if (expected.isBlank()) {
            return null;
        }
        if (expected.equals(normalize(modelConfig.getModelName()))) {
            return "modelName";
        }
        if (expected.equals(normalize(modelConfig.getName()))) {
            return "name";
        }
        Object rawAliases = modelConfig.getMetadata() == null ? null : modelConfig.getMetadata().get("aliases");
        if (rawAliases instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && expected.equals(normalize(String.valueOf(item)))) {
                    return "alias";
                }
            }
        }
        if (rawAliases instanceof String aliases) {
            for (String item : aliases.split(",")) {
                if (expected.equals(normalize(item))) {
                    return "alias";
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String decryptRequiredApiKey(ConnectionConfig connection) {
        if (connection.getEncryptedApiKey() == null || connection.getEncryptedApiKey().isBlank()) {
            throw new BizException(400, "连接未配置密钥：" + connection.getName());
        }
        return apiKeyCryptoService.decrypt(connection.getEncryptedApiKey());
    }

    private String requireArkApiKey() {
        if (arkProperties.getApiKey() == null || arkProperties.getApiKey().isBlank()) {
            throw new BizException(500, "服务未配置ARK_API_KEY，请联系管理员");
        }
        return arkProperties.getApiKey();
    }

    private int requestImageSize() {
        return 1;
    }

    private int mapProviderStatus(ProviderGatewayException ex) {
        if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
            return 502;
        }
        if (ex.getStatusCode() == 408 || ex.getStatusCode() == 504) {
            return 504;
        }
        return 502;
    }

    private String parseArkVideoUrl(Map<String, Object> body, boolean strict) {
        if (body == null) {
            if (strict) {
                throw new BizException(502, "视频模型服务返回为空");
            }
            return null;
        }
        List<String> urls = collectVideoUrls(body);
        if (!urls.isEmpty()) {
            return urls.get(0);
        }
        if (strict) {
            throw new BizException(502, "视频模型服务返回异常，缺少可用视频地址");
        }
        return null;
    }

    private List<String> collectVideoUrls(Map<String, Object> body) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        collectCandidateUrls(body, urls, 0);
        return new ArrayList<>(urls);
    }

    private void collectCandidateUrls(Object node, Set<String> urls, int depth) {
        if (node == null || depth > 8) {
            return;
        }
        String directValue = valueAsString(node);
        if (isValidMediaUrl(directValue)) {
            urls.add(directValue);
            return;
        }
        if (node instanceof Map<?, ?> mapNode) {
            for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).toLowerCase();
                Object value = entry.getValue();
                if (key.contains("url")) {
                    String maybe = valueAsString(value);
                    if (isValidMediaUrl(maybe)) {
                        urls.add(maybe);
                    }
                    if (value instanceof List<?> || value instanceof Map<?, ?>) {
                        collectCandidateUrls(value, urls, depth + 1);
                    }
                    continue;
                }
                if (value instanceof List<?> || value instanceof Map<?, ?>) {
                    collectCandidateUrls(value, urls, depth + 1);
                }
            }
            return;
        }
        if (node instanceof List<?> listNode) {
            for (Object item : listNode) {
                collectCandidateUrls(item, urls, depth + 1);
            }
        }
    }

    private boolean isValidMediaUrl(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("https://") || normalized.startsWith("http://");
    }

    private String parseArkVideoTaskId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        String taskId = firstNonBlank(
                valueAsString(body.get("task_id")),
                valueAsString(body.get("taskId")),
                valueAsString(body.get("id")),
                nestedValue(body, "data", "task_id"),
                nestedValue(body, "data", "id"),
                nestedValue(body, "output", "task_id"),
                nestedValue(body, "result", "task_id")
        );
        if (taskId != null) {
            return taskId;
        }
        Object dataNode = body.get("data");
        if (dataNode instanceof List<?> dataList) {
            for (Object item : dataList) {
                if (item instanceof Map<?, ?> mapItem) {
                    String maybe = firstNonBlank(
                            valueAsString(mapItem.get("task_id")),
                            valueAsString(mapItem.get("id"))
                    );
                    if (maybe != null) {
                        return maybe;
                    }
                }
            }
        }
        return null;
    }

    private String parseArkTaskStatus(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return firstNonBlank(
                valueAsString(body.get("status")),
                valueAsString(body.get("task_status")),
                nestedValue(body, "output", "status"),
                nestedValue(body, "data", "status"),
                nestedValue(body, "result", "status")
        );
    }

    private String parseArkTaskError(Map<String, Object> body) {
        if (body == null) {
            return "未知错误";
        }
        return firstNonBlank(
                valueAsString(body.get("message")),
                valueAsString(body.get("error")),
                valueAsString(body.get("error_message")),
                nestedValue(body, "data", "message"),
                nestedValue(body, "result", "message"),
                "未知错误"
        );
    }

    private boolean isSuccessStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return normalized.equals("success")
                || normalized.equals("succeeded")
                || normalized.equals("done")
                || normalized.equals("completed")
                || normalized.equals("finish")
                || normalized.equals("finished");
    }

    private boolean isFailedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return normalized.equals("failed")
                || normalized.equals("error")
                || normalized.equals("cancelled")
                || normalized.equals("canceled")
                || normalized.equals("timeout");
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

    private String safeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "unknown";
        }
        return status.trim();
    }

    private String summarizeBody(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return "empty";
        }
        String summary = firstNonBlank(
                valueAsString(body.get("message")),
                valueAsString(body.get("error")),
                valueAsString(body.get("code"))
        );
        if (summary != null) {
            return summary;
        }
        String raw = String.valueOf(body);
        return raw.length() <= 160 ? raw : raw.substring(0, 160);
    }

    private GenerateResponseData toData(GenerationTask task) {
        return new GenerateResponseData(
                task.getTaskId(),
                task.getStatus(),
                task.getTextResults() == null ? List.of() : task.getTextResults(),
                task.getImageResults() == null ? List.of() : task.getImageResults(),
                task.getVideoResults() == null ? List.of() : task.getVideoResults(),
                task.getCreatedAt().format(FORMATTER),
                task.getLatencyMs() == null ? 0 : task.getLatencyMs(),
                task.getPrompt(),
                task.getMode(),
                task.getStyle(),
                task.getImageModel(),
                task.getVideoModel(),
                task.getImageModelSource(),
                task.getVideoModelSource(),
                task.getImageModelMatchedBy(),
                task.getVideoModelMatchedBy(),
                task.getImageModelRejectReason(),
                task.getVideoModelRejectReason()
        );
    }

    private record MediaResult(
            String modelName,
            List<String> results,
            String modelSource,
            String matchedBy,
            String rejectReason
    ) {
    }

    private record ResolvedModel(
            ModelConfig model,
            ConnectionConfig connection,
            ProviderCatalog.ProviderDefinition provider,
            String apiKey,
            Map<String, Object> metadataPlain,
            String source,
            String matchedBy,
            String rejectReason
    ) {
        private ResolvedModel withMatch(String match) {
            return new ResolvedModel(model, connection, provider, apiKey, metadataPlain, source, match, rejectReason);
        }
    }
}
