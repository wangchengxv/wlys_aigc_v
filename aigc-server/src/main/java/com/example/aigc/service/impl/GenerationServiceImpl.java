package com.example.aigc.service.impl;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.constants.WorkspaceConstants;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.entity.GenerationTask;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
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
import com.example.aigc.service.LocalAssetFileService;
import com.example.aigc.service.ModelCapabilityService;
import com.example.aigc.service.ScriptProjectService;
import com.example.aigc.service.GatewayKind;
import com.example.aigc.service.ProviderCatalog;
import com.example.aigc.service.ProviderGatewayException;
import com.example.aigc.service.ProviderHttpGateway;
import com.example.aigc.service.RouterRoutingService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GenerationServiceImpl implements GenerationService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<String> BLOCK_WORDS = List.of("暴恐", "色情", "违禁", "涉政");
    /** 视频：全局风格 + 用户描述合并后最大长度（避免超出模型侧限制） */
    private static final int MAX_VIDEO_MERGED_PROMPT_CHARS = 8000;
    private static final int VIDU_MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int KLING_MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final Map<String, Object> EMPTY_MAP = Map.of();
    private static final Set<String> VIDU_OPTION_KEYS = Set.of(
            "duration", "seed", "resolution", "movement_amplitude", "payload", "off_peak",
            "watermark", "wm_position", "wm_url", "meta_data", "callback_url",
            "audio", "audio_type", "voice_id", "is_rec", "bgm"
    );
    private static final Set<String> VIDU_AUDIO_TYPES = Set.of("all", "speech_only", "sound_effect_only");

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
    private final LocalAssetFileService localAssetFileService;
    private final ScriptProjectService scriptProjectService;

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
            VideoStylePresetRegistry videoStylePresetRegistry,
            LocalAssetFileService localAssetFileService,
            ScriptProjectService scriptProjectService
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
        this.localAssetFileService = localAssetFileService;
        this.scriptProjectService = scriptProjectService;
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
            NormalizedAdvancedMedia normalizedAdvancedMedia = normalizeAdvancedMedia(request);
            if (request.mode() == GenerateMode.text || request.mode() == GenerateMode.both) {
                textResults = generateTextContent(request.prompt(), styleKey, request.textLength());
            }
            if (request.mode() == GenerateMode.image || request.mode() == GenerateMode.both) {
                MediaResult imageResult = generateImages(
                        request.prompt(),
                        safeCount(request.count()),
                        request.imageModel(),
                        normalizedAdvancedMedia
                );
                imageResults = imageResult.results();
                task.setImageModel(imageResult.modelName());
                task.setImageModelSource(imageResult.modelSource());
                task.setImageModelMatchedBy(imageResult.matchedBy());
                task.setImageModelRejectReason(imageResult.rejectReason());
            }
            if (request.mode() == GenerateMode.video) {
                String videoPrompt = buildVideoPrompt(styleKey, request.prompt());
                validateVideoMergedPromptLength(videoPrompt);
                MediaResult videoResult = generateVideos(
                        videoPrompt,
                        safeCount(request.count()),
                        request.videoModel(),
                        normalizedAdvancedMedia.videoReferenceImageUrl(),
                        normalizedAdvancedMedia.videoViduOptions()
                );
                videoResults = videoResult.results();
                task.setVideoModel(videoResult.modelName());
                task.setVideoModelSource(videoResult.modelSource());
                task.setVideoModelMatchedBy(videoResult.matchedBy());
                task.setVideoModelRejectReason(videoResult.rejectReason());
            }
            task.setTextResults(textResults);
            task.setImageResults(imageResults);
            task.setVideoResults(videoResults);
            persistWorkspaceGenerationFiles(taskId, task, imageResults, videoResults);
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

    private MediaResult generateImages(
            String prompt,
            int count,
            String requestedModel,
            NormalizedAdvancedMedia normalizedAdvancedMedia
    ) {
        ResolvedModel resolvedModel = resolveModel("image", requestedModel);
        if (resolvedModel != null) {
            return new MediaResult(
                    resolvedModel.model().getModelName(),
                    generateImagesWithConfiguredModel(prompt, count, resolvedModel, normalizedAdvancedMedia),
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

    private MediaResult generateVideos(
            String prompt,
            int count,
            String requestedModel,
            String videoReferenceImageUrl,
            Map<String, Object> videoViduOptions
    ) {
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
            if ("vidu".equalsIgnoreCase(resolvedModel.provider().key())) {
                return new MediaResult(
                        resolvedModel.model().getModelName(),
                        generateVideosWithViduConnection(prompt, count, resolvedModel, videoReferenceImageUrl, videoViduOptions),
                        resolvedModel.source(),
                        resolvedModel.matchedBy(),
                        resolvedModel.rejectReason()
                );
            }
            if ("vidu_onelink".equalsIgnoreCase(resolvedModel.provider().key())) {
                return new MediaResult(
                        resolvedModel.model().getModelName(),
                        generateVideosWithViduOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl, videoViduOptions),
                        resolvedModel.source(),
                        resolvedModel.matchedBy(),
                        resolvedModel.rejectReason()
                );
            }
            if ("kling".equalsIgnoreCase(resolvedModel.provider().key())) {
                return new MediaResult(
                        resolvedModel.model().getModelName(),
                        generateVideosWithKlingOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl),
                        resolvedModel.source(),
                        resolvedModel.matchedBy(),
                        resolvedModel.rejectReason()
                );
            }
            if ("onelinkai".equalsIgnoreCase(resolvedModel.provider().key())) {
                if (isViduWorkspaceModel(resolvedModel.model().getModelName())) {
                    return new MediaResult(
                            resolvedModel.model().getModelName(),
                            generateVideosWithViduOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl, videoViduOptions),
                            resolvedModel.source(),
                            resolvedModel.matchedBy(),
                            resolvedModel.rejectReason()
                    );
                }
                if (isKlingModel(resolvedModel.model().getModelName())) {
                    return new MediaResult(
                            resolvedModel.model().getModelName(),
                            generateVideosWithKlingOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl),
                            resolvedModel.source(),
                            resolvedModel.matchedBy(),
                            resolvedModel.rejectReason()
                    );
                }
                throw new BizException(400, "当前 OneLink 视频模型仅支持 Vidu 或 Kling；其它模型请改用方舟/专属连接或补充接入");
            }
            throw new BizException(400, "当前视频模型仅支持配置为方舟(ark)、Moark(moark)、Vidu(vidu)、Vidu OneLink(vidu_onelink)、Kling(kling) 或 OneLink+Vidu/Kling 连接");
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

    private NormalizedAdvancedMedia normalizeAdvancedMedia(GenerateRequest request) {
        Map<String, Object> advancedMedia = sanitizeObjectMap(request.advancedMedia());
        Map<String, Object> imageSection = sanitizeObjectMap(advancedMedia.get("image"));
        Map<String, Object> imageExtra = sanitizeObjectMap(imageSection.get("extra"));
        Map<String, Object> reference2image = sanitizeObjectMap(imageExtra.get("reference2image"));
        Map<String, Object> klingMultiReference = sanitizeObjectMap(imageExtra.get("klingMultiReference"));
        Map<String, Object> outpaint = sanitizeObjectMap(imageExtra.get("outpaint"));
        Map<String, Object> omni = sanitizeObjectMap(imageExtra.get("omni"));
        Map<String, Object> videoSection = sanitizeObjectMap(advancedMedia.get("video"));

        List<String> reference2imageImages = firstNonEmptyStringList(
                sanitizeStringList(reference2image.get("images"))
        );
        String imageReferenceImageUrl = firstNonBlank(
                valueAsString(imageSection.get("referenceImageUrl")),
                valueAsString(reference2image.get("referenceImageUrl")),
                firstItem(reference2imageImages)
        );
        if (reference2imageImages.isEmpty() && imageReferenceImageUrl != null && !imageReferenceImageUrl.isBlank()) {
            reference2imageImages = List.of(imageReferenceImageUrl);
        }

        List<String> klingReferenceImages = firstNonEmptyStringList(
                sanitizeStringList(klingMultiReference.get("referenceImageUrls")),
                sanitizeStringList(klingMultiReference.get("images"))
        );

        String outpaintSourceImageUrl = firstNonBlank(
                valueAsString(outpaint.get("sourceImageUrl")),
                valueAsString(outpaint.get("image"))
        );
        Integer outpaintTop = parseIntegerStrict(outpaint.get("top"));
        Integer outpaintRight = parseIntegerStrict(outpaint.get("right"));
        Integer outpaintBottom = parseIntegerStrict(outpaint.get("bottom"));
        Integer outpaintLeft = parseIntegerStrict(outpaint.get("left"));

        String omniSourceImageUrl = firstNonBlank(
                valueAsString(omni.get("sourceImageUrl")),
                valueAsString(omni.get("image"))
        );
        String omniMode = valueAsString(omni.get("mode"));
        String omniSubjectPrompt = valueAsString(omni.get("subjectPrompt"));

        String imageCapability = normalizeImageAdvancedCapability(firstNonBlank(
                valueAsString(imageExtra.get("capability")),
                !reference2image.isEmpty() || imageReferenceImageUrl != null && !imageReferenceImageUrl.isBlank()
                        ? "vidu_reference2image" : null,
                !klingMultiReference.isEmpty() || !klingReferenceImages.isEmpty()
                        ? "kling_multi_reference" : null,
                !outpaint.isEmpty() || outpaintSourceImageUrl != null && !outpaintSourceImageUrl.isBlank()
                        ? "outpaint" : null,
                !omni.isEmpty() || omniSourceImageUrl != null && !omniSourceImageUrl.isBlank()
                        ? "omni" : null,
                inferImageAdvancedCapability(request.imageModel())
        ));

        String videoReferenceImageUrl = firstNonBlank(
                valueAsString(videoSection.get("referenceImageUrl")),
                valueAsString(videoSection.get("videoReferenceImageUrl")),
                request.videoReferenceImageUrl()
        );

        Map<String, Object> mergedViduOptions = new HashMap<>(extractViduOptions(request.videoViduOptions()));
        mergedViduOptions.putAll(extractViduOptions(sanitizeObjectMap(videoSection.get("videoViduOptions"))));
        mergedViduOptions.putAll(extractViduOptions(sanitizeObjectMap(videoSection.get("viduOptions"))));

        return new NormalizedAdvancedMedia(
                new NormalizedImageRequest(
                        imageCapability,
                        imageReferenceImageUrl,
                        imageCapability != null && imageCapability.equals("kling_multi_reference")
                                ? klingReferenceImages
                                : reference2imageImages,
                        imageCapability != null && imageCapability.equals("outpaint") ? outpaintSourceImageUrl : omniSourceImageUrl,
                        outpaintTop,
                        outpaintRight,
                        outpaintBottom,
                        outpaintLeft,
                        omniMode,
                        omniSubjectPrompt
                ),
                videoReferenceImageUrl,
                mergedViduOptions.isEmpty() ? EMPTY_MAP : mergedViduOptions
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

    private List<String> generateImagesWithConfiguredModel(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            NormalizedAdvancedMedia normalizedAdvancedMedia
    ) {
        NormalizedImageRequest imageRequest = normalizedAdvancedMedia == null ? null : normalizedAdvancedMedia.image();
        if (imageRequest != null && imageRequest.hasAdvancedRequest()) {
            return switch (imageRequest.capability()) {
                case "vidu_reference2image" -> generateImagesWithViduReference2Image(prompt, count, resolvedModel, imageRequest);
                case "kling_multi_reference" -> generateImagesWithKlingMultiReference(prompt, count, resolvedModel, imageRequest);
                case "outpaint" -> generateImagesWithKlingOutpaint(prompt, count, resolvedModel, imageRequest);
                case "omni" -> generateImagesWithKlingOmni(prompt, count, resolvedModel, imageRequest);
                default -> throw new BizException(400, "不支持的图片高级能力：" + imageRequest.capability());
            };
        }
        ProviderCatalog.ProviderDefinition provider = resolvedModel.provider();
        if ("onelinkai".equalsIgnoreCase(provider.key()) && isKlingModel(resolvedModel.model().getModelName())) {
            return generateImagesWithKlingOneLinkConnection(prompt, count, resolvedModel);
        }
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

    private List<String> generateImagesWithViduReference2Image(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            NormalizedImageRequest imageRequest
    ) {
        List<String> refs = imageRequest.referenceImages();
        if (refs.isEmpty()) {
            throw new BizException(400, "Vidu reference2image 需要至少 1 张参考图");
        }
        if (refs.size() > 7) {
            throw new BizException(400, "Vidu reference2image 最多支持 7 张参考图");
        }
        List<String> normalizedRefs = new ArrayList<>();
        for (String ref : refs) {
            if (ref == null || ref.isBlank()) {
                continue;
            }
            String trimmed = ref.trim();
            validateViduImageRef(trimmed);
            normalizedRefs.add(trimmed);
        }
        if (normalizedRefs.isEmpty()) {
            throw new BizException(400, "Vidu reference2image 需要至少 1 张参考图");
        }
        List<String> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            images.addAll(callViduReference2ImageApi(
                    resolveViduImageProvider(resolvedModel.provider()),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    buildViduReference2ImagePayload(resolvedModel, prompt, normalizedRefs)
            ));
        }
        return images;
    }

    private List<String> generateImagesWithKlingMultiReference(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            NormalizedImageRequest imageRequest
    ) {
        ensureOneLinkImageAdvancedProvider(resolvedModel, "Kling 多图参考生图");
        Map<String, Object> payload = buildKlingMultiReferencePayload(resolvedModel, prompt, count, imageRequest);
        return callKlingImageTaskApi(
                resolvedModel.provider(),
                resolvedModel.connection().getBaseUrl(),
                resolvedModel.apiKey(),
                resolvedModel.metadataPlain(),
                payload,
                "/kling/v1/images/multi-image2image",
                "/kling/v1/images/multi-image2image/{taskId}",
                "Kling 多图参考生图"
        );
    }

    private List<String> generateImagesWithKlingOutpaint(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            NormalizedImageRequest imageRequest
    ) {
        ensureOneLinkImageAdvancedProvider(resolvedModel, "Kling 扩图");
        Map<String, Object> payload = buildKlingOutpaintPayload(prompt, count, imageRequest);
        return callKlingImageTaskApi(
                resolvedModel.provider(),
                resolvedModel.connection().getBaseUrl(),
                resolvedModel.apiKey(),
                resolvedModel.metadataPlain(),
                payload,
                "/kling/v1/images/editing/expand",
                "/kling/v1/images/editing/expand/{taskId}",
                "Kling 扩图"
        );
    }

    private List<String> generateImagesWithKlingOmni(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            NormalizedImageRequest imageRequest
    ) {
        ensureOneLinkImageAdvancedProvider(resolvedModel, "Kling Omni");
        Map<String, Object> payload = buildKlingOmniPayload(resolvedModel, prompt, count, imageRequest);
        return callKlingImageTaskApi(
                resolvedModel.provider(),
                resolvedModel.connection().getBaseUrl(),
                resolvedModel.apiKey(),
                resolvedModel.metadataPlain(),
                payload,
                "/kling/v1/images/omni-image",
                "/kling/v1/images/omni-image/{taskId}",
                "Kling Omni"
        );
    }

    private List<String> generateImagesWithKlingOneLinkConnection(String prompt, int count, ResolvedModel resolvedModel) {
        List<String> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            images.add(callKlingImageApi(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    prompt,
                    resolvedModel.model().getModelName()
            ));
        }
        return images;
    }

    private ProviderCatalog.ProviderDefinition resolveViduImageProvider(ProviderCatalog.ProviderDefinition provider) {
        if ("vidu".equalsIgnoreCase(provider.key())) {
            return provider;
        }
        if ("onelinkai".equalsIgnoreCase(provider.key())) {
            return providerCatalog.require("vidu_onelink");
        }
        throw new BizException(400, "当前图片模型对应连接不支持 Vidu reference2image");
    }

    private void ensureOneLinkImageAdvancedProvider(ResolvedModel resolvedModel, String capabilityName) {
        if (!"onelinkai".equalsIgnoreCase(resolvedModel.provider().key())) {
            throw new BizException(400, capabilityName + " 当前仅支持通过 OneLink/Kling 连接调用");
        }
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

    private List<String> generateVideosWithViduConnection(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            String referenceImageUrl,
            Map<String, Object> rawOptions
    ) {
        if (referenceImageUrl == null || referenceImageUrl.isBlank()) {
            throw new BizException(400, "Vidu 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址，或 data:image/...;base64,...）");
        }
        String imageRef = referenceImageUrl.trim();
        validateViduImageRef(imageRef);
        Map<String, Object> payload = buildViduPayload(resolvedModel, prompt, imageRef, rawOptions);
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            videos.addAll(callViduVideoApi(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload
            ));
        }
        return videos;
    }

    private List<String> generateVideosWithViduOneLinkConnection(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            String referenceImageUrl,
            Map<String, Object> rawOptions
    ) {
        if (referenceImageUrl == null || referenceImageUrl.isBlank()) {
            throw new BizException(400, "Vidu 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址，或 data:image/...;base64,...）");
        }
        String imageRef = referenceImageUrl.trim();
        validateViduImageRef(imageRef);
        Map<String, Object> payload = buildViduPayload(resolvedModel, prompt, imageRef, rawOptions);
        ProviderCatalog.ProviderDefinition viduDef = providerCatalog.require("vidu_onelink");
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            videos.addAll(callViduVideoApi(
                    viduDef,
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload
            ));
        }
        return videos;
    }

    private List<String> generateVideosWithKlingOneLinkConnection(
            String prompt,
            int count,
            ResolvedModel resolvedModel,
            String referenceImageUrl
    ) {
        String imageRef = referenceImageUrl == null ? "" : referenceImageUrl.trim();
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            videos.addAll(callKlingVideoApi(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    prompt,
                    resolvedModel.model().getModelName(),
                    imageRef
            ));
        }
        return videos;
    }

    private boolean isViduWorkspaceModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return false;
        }
        String n = modelName.trim().toLowerCase(Locale.ROOT);
        return n.startsWith("viduq") || n.startsWith("vidu");
    }

    private boolean isKlingModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return false;
        }
        return modelName.trim().toLowerCase(Locale.ROOT).startsWith("kling-");
    }

    private boolean isValidViduRefImage(String ref) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        String t = ref.trim();
        if (isValidMediaUrl(t)) {
            return true;
        }
        String low = t.toLowerCase(Locale.ROOT);
        return low.startsWith("data:image/") && t.contains("base64");
    }

    private List<String> callViduVideoApi(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload
    ) {
        try {
            Map<String, Object> submitBody = providerHttpGateway.submitVideoTask(
                    provider,
                    baseUrl,
                    apiKey,
                    connectionMetadata,
                    payload,
                    Duration.ofSeconds(120)
            );
            List<String> direct = flattenViduVideoUrls(submitBody);
            if (!direct.isEmpty()) {
                return direct;
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "Vidu 未返回 task_id 或视频地址");
            }
            return pollViduVideoTask(provider, baseUrl, apiKey, taskId);
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private List<String> callViduReference2ImageApi(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload
    ) {
        String path = "vidu_onelink".equalsIgnoreCase(provider.key())
                ? "/vidu/ent/v2/reference2image"
                : "/ent/v2/reference2image";
        try {
            Map<String, Object> submitBody = providerHttpGateway.postJson(
                    baseUrl,
                    path,
                    provider,
                    apiKey,
                    connectionMetadata,
                    payload,
                    Duration.ofSeconds(120)
            );
            List<String> direct = flattenViduImageUrls(submitBody);
            if (!direct.isEmpty()) {
                return direct;
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "Vidu reference2image 未返回 task_id 或图片地址");
            }
            return pollViduImageTask(provider, baseUrl, apiKey, taskId);
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private String callKlingImageApi(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            String prompt,
            String modelName
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model_name", modelName);
        payload.put("prompt", prompt);
        payload.put("n", 1);
        try {
            Map<String, Object> submitBody = providerHttpGateway.postJson(
                    baseUrl,
                    "/kling/v1/images/generations",
                    provider,
                    apiKey,
                    connectionMetadata,
                    payload,
                    Duration.ofSeconds(120)
            );
            List<String> direct = extractKlingTaskResultUrls(submitBody, "images");
            if (!direct.isEmpty()) {
                return direct.get(0);
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "Kling 图片生成未返回 task_id");
            }
            List<String> urls = pollKlingTaskForUrls(
                    provider,
                    baseUrl,
                    apiKey,
                    connectionMetadata,
                    "/kling/v1/images/generations/{taskId}",
                    taskId,
                    "images"
            );
            if (urls.isEmpty()) {
                throw new BizException(502, "Kling 图片任务已完成，但未返回图片地址");
            }
            return urls.get(0);
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private List<String> callKlingImageTaskApi(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            String submitPath,
            String queryPath,
            String capabilityName
    ) {
        try {
            Map<String, Object> submitBody = providerHttpGateway.postJson(
                    baseUrl,
                    submitPath,
                    provider,
                    apiKey,
                    connectionMetadata,
                    payload,
                    Duration.ofSeconds(120)
            );
            List<String> direct = extractKlingTaskResultUrls(submitBody, "images");
            if (!direct.isEmpty()) {
                return direct;
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, capabilityName + " 未返回 task_id");
            }
            List<String> urls = pollKlingTaskForUrls(
                    provider,
                    baseUrl,
                    apiKey,
                    connectionMetadata,
                    queryPath,
                    taskId,
                    "images"
            );
            if (urls.isEmpty()) {
                throw new BizException(502, capabilityName + " 任务已完成，但未返回图片地址");
            }
            return urls;
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private List<String> callKlingVideoApi(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            String prompt,
            String modelName,
            String referenceImageUrl
    ) {
        boolean imageToVideo = referenceImageUrl != null && !referenceImageUrl.isBlank();
        String submitPath = imageToVideo ? "/kling/v1/videos/image2video" : "/kling/v1/videos/text2video";
        String queryPath = imageToVideo ? "/kling/v1/videos/image2video/{taskId}" : "/kling/v1/videos/text2video/{taskId}";
        Map<String, Object> payload = new HashMap<>();
        payload.put("model_name", modelName);
        payload.put("duration", String.valueOf(safeVideoDuration()));
        if (prompt != null && !prompt.isBlank()) {
            payload.put("prompt", prompt);
        }
        if (imageToVideo) {
            payload.put("image", referenceImageUrl);
        }
        try {
            Map<String, Object> submitBody = providerHttpGateway.postJson(
                    baseUrl,
                    submitPath,
                    provider,
                    apiKey,
                    connectionMetadata,
                    payload,
                    Duration.ofSeconds(120)
            );
            List<String> direct = extractKlingTaskResultUrls(submitBody, "videos");
            if (!direct.isEmpty()) {
                return direct;
            }
            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "Kling 视频生成未返回 task_id");
            }
            return pollKlingTaskForUrls(provider, baseUrl, apiKey, connectionMetadata, queryPath, taskId, "videos");
        } catch (ProviderGatewayException ex) {
            throw new BizException(mapProviderStatus(ex), ex.getMessage());
        }
    }

    private List<String> pollKlingTaskForUrls(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            String resultPathTemplate,
            String taskId,
            String resultField
    ) {
        int maxAttempts = Math.max(1, arkProperties.getVideoPollMaxAttempts());
        long intervalMs = Math.max(300L, arkProperties.getVideoPollIntervalMs());
        Map<String, Object> lastBody = null;
        String lastStatus = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> resultBody;
            try {
                resultBody = providerHttpGateway.getJson(
                        baseUrl,
                        pathWithTaskId(resultPathTemplate, taskId),
                        provider,
                        apiKey,
                        connectionMetadata,
                        Duration.ofSeconds(30)
                );
            } catch (ProviderGatewayException ex) {
                throw new BizException(mapProviderStatus(ex), ex.getMessage());
            }
            lastBody = resultBody;
            List<String> urls = extractKlingTaskResultUrls(resultBody, resultField);
            if (!urls.isEmpty()) {
                return urls;
            }
            String status = parseArkTaskStatus(resultBody);
            lastStatus = status;
            if (isFailedStatus(status)) {
                throw new BizException(502, "任务失败：" + parseArkTaskError(resultBody));
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new BizException(504, "任务轮询被中断，请稍后重试");
                }
            }
        }
        if (isSuccessStatus(lastStatus)) {
            throw new BizException(502, "任务已完成，但未返回结果地址，task_id=" + taskId + "，响应摘要=" + summarizeBody(lastBody));
        }
        throw new BizException(504, "任务处理超时，请稍后重试");
    }

    private List<String> extractKlingTaskResultUrls(Map<String, Object> body, String resultField) {
        if (body == null || resultField == null || resultField.isBlank()) {
            return List.of();
        }
        Object data = body.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return List.of();
        }
        Object taskResult = dataMap.get("task_result");
        if (!(taskResult instanceof Map<?, ?> taskResultMap)) {
            return List.of();
        }
        Object mediaNode = taskResultMap.get(resultField);
        if (mediaNode == null) {
            return List.of();
        }
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        collectCandidateUrls(mediaNode, urls, 0);
        return new ArrayList<>(urls);
    }

    private String pathWithTaskId(String template, String taskId) {
        return template.replace("{taskId}", taskId);
    }

    private List<String> pollViduVideoTask(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            String taskId
    ) {
        int maxAttempts = Math.max(1, arkProperties.getVideoPollMaxAttempts());
        long intervalMs = Math.max(300L, arkProperties.getVideoPollIntervalMs());
        Map<String, Object> lastBody = null;
        String lastStatus = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> resultBody;
            try {
                resultBody = providerHttpGateway.queryVideoTask(provider, baseUrl, apiKey, taskId, Duration.ofSeconds(30));
            } catch (ProviderGatewayException ex) {
                throw new BizException(mapProviderStatus(ex), ex.getMessage());
            }
            lastBody = resultBody;
            Object errNode = resultBody.get("error");
            if (errNode != null && !Boolean.FALSE.equals(errNode) && !"false".equalsIgnoreCase(String.valueOf(errNode).trim())) {
                throw new BizException(502, parseViduTaskError(resultBody));
            }
            List<String> urls = flattenViduVideoUrls(resultBody);
            if (!urls.isEmpty()) {
                return urls;
            }
            String status = parseArkTaskStatus(resultBody);
            lastStatus = status;
            if (isFailedStatus(status)) {
                throw new BizException(502, "视频任务失败：" + parseViduTaskError(resultBody));
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
            throw new BizException(502, "视频任务已完成，但未返回视频地址或水印地址，task_id="
                    + taskId + "，status=" + lastStatus + "，响应摘要=" + summarizeBody(lastBody));
        }
        throw new BizException(504, "视频生成超时，请稍后重试或缩短提示词");
    }

    private List<String> pollViduImageTask(
            ProviderCatalog.ProviderDefinition provider,
            String baseUrl,
            String apiKey,
            String taskId
    ) {
        int maxAttempts = Math.max(1, arkProperties.getVideoPollMaxAttempts());
        long intervalMs = Math.max(300L, arkProperties.getVideoPollIntervalMs());
        Map<String, Object> lastBody = null;
        String lastStatus = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> resultBody;
            try {
                resultBody = providerHttpGateway.queryVideoTask(provider, baseUrl, apiKey, taskId, Duration.ofSeconds(30));
            } catch (ProviderGatewayException ex) {
                throw new BizException(mapProviderStatus(ex), ex.getMessage());
            }
            lastBody = resultBody;
            Object errNode = resultBody.get("error");
            if (errNode != null && !Boolean.FALSE.equals(errNode) && !"false".equalsIgnoreCase(String.valueOf(errNode).trim())) {
                throw new BizException(502, parseViduTaskError(resultBody));
            }
            List<String> urls = flattenViduImageUrls(resultBody);
            if (!urls.isEmpty()) {
                return urls;
            }
            String status = parseArkTaskStatus(resultBody);
            lastStatus = status;
            if (isFailedStatus(status)) {
                throw new BizException(502, "图片任务失败：" + parseViduTaskError(resultBody));
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new BizException(504, "图片任务轮询被中断，请稍后重试");
                }
            }
        }
        if (isSuccessStatus(lastStatus)) {
            throw new BizException(502, "图片任务已完成，但未返回图片地址，task_id="
                    + taskId + "，status=" + lastStatus + "，响应摘要=" + summarizeBody(lastBody));
        }
        throw new BizException(504, "图片生成超时，请稍后重试");
    }

    private Map<String, Object> buildViduPayload(
            ResolvedModel resolvedModel,
            String prompt,
            String imageRef,
            Map<String, Object> rawOptions
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", resolvedModel.model().getModelName());
        payload.put("images", List.of(imageRef));
        if (prompt != null && !prompt.isBlank()) {
            payload.put("prompt", prompt.trim());
        }
        payload.putAll(extractViduOptions(rawOptions));
        validateAndNormalizeViduOptions(payload, resolvedModel.model().getMetadata(), resolvedModel.model().getModelName());
        return payload;
    }

    private Map<String, Object> buildViduReference2ImagePayload(
            ResolvedModel resolvedModel,
            String prompt,
            List<String> imageRefs
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", resolvedModel.model().getModelName());
        payload.put("images", imageRefs);
        if (prompt != null && !prompt.isBlank()) {
            payload.put("prompt", prompt.trim());
        }
        return payload;
    }

    private Map<String, Object> buildKlingMultiReferencePayload(
            ResolvedModel resolvedModel,
            String prompt,
            int count,
            NormalizedImageRequest imageRequest
    ) {
        List<String> refs = imageRequest.referenceImages();
        if (refs.size() < 2 || refs.size() > 4) {
            throw new BizException(400, "Kling 多图参考生图需要 2~4 张参考图");
        }
        List<Map<String, Object>> subjectImageList = new ArrayList<>();
        for (String ref : refs) {
            subjectImageList.add(Map.of("subject_image", normalizeKlingImageInput(ref)));
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("model_name", resolvedModel.model().getModelName());
        payload.put("subject_image_list", subjectImageList);
        payload.put("n", count);
        if (prompt != null && !prompt.isBlank()) {
            payload.put("prompt", prompt.trim());
        }
        return payload;
    }

    private Map<String, Object> buildKlingOutpaintPayload(
            String prompt,
            int count,
            NormalizedImageRequest imageRequest
    ) {
        String image = imageRequest.sourceImageUrl();
        if (image == null || image.isBlank()) {
            throw new BizException(400, "扩图需要提供原图");
        }
        int top = requireNonNegativeExpand(imageRequest.outpaintTop(), "top");
        int right = requireNonNegativeExpand(imageRequest.outpaintRight(), "right");
        int bottom = requireNonNegativeExpand(imageRequest.outpaintBottom(), "bottom");
        int left = requireNonNegativeExpand(imageRequest.outpaintLeft(), "left");
        if (top + right + bottom + left <= 0) {
            throw new BizException(400, "扩图至少需要一个大于 0 的扩边值");
        }
        ImageDimensions dimensions = readImageDimensions(image);
        double upRatio = top / (double) dimensions.height();
        double downRatio = bottom / (double) dimensions.height();
        double leftRatio = left / (double) dimensions.width();
        double rightRatio = right / (double) dimensions.width();
        validateOutpaintRatio(upRatio, "top");
        validateOutpaintRatio(downRatio, "bottom");
        validateOutpaintRatio(leftRatio, "left");
        validateOutpaintRatio(rightRatio, "right");
        double areaMultiplier = (1d + leftRatio + rightRatio) * (1d + upRatio + downRatio);
        if (areaMultiplier > 3.0d + 1e-9) {
            throw new BizException(400, "扩图后整体面积不能超过原图 3 倍，请减小扩边值");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("image", normalizeKlingImageInput(image));
        payload.put("up_expansion_ratio", upRatio);
        payload.put("down_expansion_ratio", downRatio);
        payload.put("left_expansion_ratio", leftRatio);
        payload.put("right_expansion_ratio", rightRatio);
        payload.put("n", count);
        if (prompt != null && !prompt.isBlank()) {
            payload.put("prompt", prompt.trim());
        }
        return payload;
    }

    private Map<String, Object> buildKlingOmniPayload(
            ResolvedModel resolvedModel,
            String prompt,
            int count,
            NormalizedImageRequest imageRequest
    ) {
        String sourceImage = imageRequest.sourceImageUrl();
        if (sourceImage == null || sourceImage.isBlank()) {
            throw new BizException(400, "Omni 需要提供输入图");
        }
        String subjectPrompt = imageRequest.omniSubjectPrompt();
        if (subjectPrompt == null || subjectPrompt.isBlank()) {
            throw new BizException(400, "Omni 需要主体描述");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("model_name", resolvedModel.model().getModelName());
        payload.put("image_list", List.of(Map.of("image", normalizeKlingImageInput(sourceImage))));
        payload.put("n", count);
        payload.put("prompt", buildKlingOmniPrompt(prompt, subjectPrompt, imageRequest.omniMode()));
        return payload;
    }

    private Map<String, Object> extractViduOptions(Map<String, Object> rawOptions) {
        if (rawOptions == null || rawOptions.isEmpty()) {
            return EMPTY_MAP;
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawOptions.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (!VIDU_OPTION_KEYS.contains(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof String text && text.trim().isBlank()) {
                continue;
            }
            out.put(key, value);
        }
        return out;
    }

    private void validateAndNormalizeViduOptions(
            Map<String, Object> payload,
            Map<String, Object> modelMetadata,
            String modelName
    ) {
        Map<String, Object> modelMeta = modelMetadata == null ? EMPTY_MAP : modelMetadata;
        String family = firstNonBlank(
                valueAsString(modelMeta.get("viduFamily")),
                detectViduModelFamily(modelName),
                "q2"
        );
        ViduMatrix matrix = defaultViduMatrix(family);
        List<Integer> allowedDurations = ensureIntList(modelMeta.get("viduDurations"), matrix.durations());
        List<String> allowedResolutions = ensureStringList(modelMeta.get("viduResolutions"), matrix.resolutions());
        boolean audioSupported = matrix.audioSupported();
        Object rawAudioSupported = modelMeta.get("viduAudioSupported");
        if (rawAudioSupported instanceof Boolean b) {
            audioSupported = b;
        }

        if (payload.containsKey("duration")) {
            Integer duration = parseIntegerStrict(payload.get("duration"));
            if (duration == null) {
                throw new BizException(400, "Vidu 参数 duration 必须为整数秒");
            }
            if (!allowedDurations.contains(duration)) {
                throw new BizException(400, "Vidu 模型族 " + family + " 不支持 duration=" + duration + "，可选：" + joinInts(allowedDurations));
            }
            payload.put("duration", duration);
        }
        if (payload.containsKey("resolution")) {
            String resolution = valueAsString(payload.get("resolution"));
            if (resolution != null && !resolution.isBlank() && !containsIgnoreCase(allowedResolutions, resolution)) {
                throw new BizException(400, "Vidu 模型族 " + family + " 不支持 resolution=" + resolution + "，可选：" + String.join(", ", allowedResolutions));
            }
            payload.put("resolution", resolution);
        }

        boolean audioEnabled = false;
        if (payload.containsKey("audio")) {
            Boolean audio = parseBooleanStrict(payload.get("audio"));
            if (audio == null) {
                throw new BizException(400, "Vidu 参数 audio 必须为布尔值");
            }
            payload.put("audio", audio);
            audioEnabled = audio;
        }
        if (!audioSupported && audioEnabled) {
            throw new BizException(400, "Vidu 模型族 " + family + " 不支持 audio=true");
        }
        if (payload.containsKey("audio_type")) {
            String audioType = valueAsString(payload.get("audio_type"));
            audioType = audioType == null ? "" : audioType.toLowerCase(Locale.ROOT);
            if (!audioType.isBlank()) {
                if (!VIDU_AUDIO_TYPES.contains(audioType)) {
                    throw new BizException(400, "Vidu 参数 audio_type 仅支持 all/speech_only/sound_effect_only");
                }
                if (!audioEnabled) {
                    throw new BizException(400, "audio=false 时不允许传 audio_type");
                }
            }
            payload.put("audio_type", audioType);
        }
        if (payload.containsKey("voice_id")) {
            String voiceId = valueAsString(payload.get("voice_id"));
            voiceId = voiceId == null ? "" : voiceId;
            if (!voiceId.isBlank() && !audioEnabled) {
                throw new BizException(400, "audio=false 时不允许传 voice_id");
            }
            payload.put("voice_id", voiceId);
        }
        if (payload.containsKey("bgm")) {
            Boolean bgm = parseBooleanStrict(payload.get("bgm"));
            if (bgm == null) {
                throw new BizException(400, "Vidu 参数 bgm 必须为布尔值");
            }
            if (bgm && !audioEnabled) {
                throw new BizException(400, "audio=false 时不允许开启 bgm");
            }
            payload.put("bgm", bgm);
        }
        if (payload.containsKey("wm_position")) {
            Integer wmPosition = parseIntegerStrict(payload.get("wm_position"));
            if (wmPosition == null || wmPosition < 1 || wmPosition > 4) {
                throw new BizException(400, "Vidu 参数 wm_position 必须在 1-4 之间");
            }
            payload.put("wm_position", wmPosition);
        }
        if (payload.containsKey("is_rec")) {
            Boolean isRec = parseBooleanStrict(payload.get("is_rec"));
            if (isRec == null) {
                throw new BizException(400, "Vidu 参数 is_rec 必须为布尔值");
            }
            payload.put("is_rec", isRec);
            if (isRec) {
                payload.remove("prompt");
                payload.put("meta_data", mergeViduMetaData(payload.get("meta_data"), Map.of("rec_mode_enabled", true)));
            }
        }
    }

    private void validateViduImageRef(String ref) {
        if (!isValidViduRefImage(ref)) {
            throw new BizException(400, "Vidu images 仅支持 1 张 http(s) 图片地址或 data:image base64");
        }
        String normalized = ref.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("data:image/")) {
            validateViduDataImage(ref);
            return;
        }
        validateViduRemoteImage(ref);
    }

    private void validateViduDataImage(String ref) {
        String[] parts = ref.split(",", 2);
        if (parts.length != 2) {
            throw new BizException(400, "Vidu data:image 格式错误");
        }
        String header = parts[0].toLowerCase(Locale.ROOT);
        if (!header.contains("image/jpeg") && !header.contains("image/jpg") && !header.contains("image/png")) {
            throw new BizException(400, "Vidu 仅支持 JPEG/PNG 参考图");
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(parts[1].trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(400, "Vidu 参考图 Base64 解码失败");
        }
        validateViduImageBytes(data);
    }

    private void validateViduRemoteImage(String imageUrl) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(imageUrl))
                    .header("User-Agent", "aigc-server/vidu-validator")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
        } catch (Exception ex) {
            throw new BizException(400, "Vidu 参考图地址不合法");
        }
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception ex) {
            throw new BizException(400, "Vidu 参考图地址不可访问");
        }
        if (response.statusCode() >= 400) {
            throw new BizException(400, "Vidu 参考图地址不可访问");
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if (contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(';')).trim();
        }
        if (!contentType.isBlank() && !"image/jpeg".equals(contentType) && !"image/jpg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new BizException(400, "Vidu 仅支持 JPEG/PNG 参考图");
        }
        validateViduImageBytes(response.body());
    }

    private void validateViduImageBytes(byte[] data) {
        if (data == null || data.length == 0 || data.length > VIDU_MAX_IMAGE_BYTES) {
            throw new BizException(400, "Vidu 参考图体积需在 0-10MB 之间");
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new BizException(400, "Vidu 参考图无法解析，请使用 JPEG/PNG 图片");
        }
        if (image == null) {
            throw new BizException(400, "Vidu 参考图无法解析，请使用 JPEG/PNG 图片");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new BizException(400, "Vidu 参考图尺寸无效");
        }
        double ratio = (double) width / (double) height;
        if (ratio < 0.4d || ratio > 2.5d) {
            throw new BizException(400, "Vidu 参考图比例不受支持，请使用 0.4-2.5 之间的宽高比");
        }
    }

    private String parseViduPrimaryVideoUrl(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object creations = body.get("creations");
        if (creations instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object u = first.get("url");
            if (u != null) {
                String s = String.valueOf(u).trim();
                if (isValidMediaUrl(s)) {
                    return s;
                }
            }
        }
        return parseArkVideoUrl(body, false);
    }

    private List<String> flattenViduImageUrls(Map<String, Object> body) {
        if (body == null) {
            return List.of();
        }
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Object creations = body.get("creations");
        if (creations != null) {
            collectCandidateUrls(creations, urls, 0);
        }
        Object data = body.get("data");
        if (data != null) {
            collectCandidateUrls(data, urls, 0);
        }
        collectCandidateUrls(body.get("images"), urls, 0);
        return new ArrayList<>(urls);
    }

    private List<String> flattenViduVideoUrls(Map<String, Object> body) {
        String primary = null;
        String watermark = null;
        if (body != null) {
            Object creations = body.get("creations");
            if (creations instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                for (Map.Entry<?, ?> entry : first.entrySet()) {
                    String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT);
                    String value = valueAsString(entry.getValue());
                    if (!isValidMediaUrl(value)) {
                        continue;
                    }
                    boolean watermarkKey = key.contains("watermark") || key.contains("wm_") || key.contains("wmurl");
                    if (watermarkKey && watermark == null) {
                        watermark = value;
                        continue;
                    }
                    boolean primaryKey = key.equals("url") || key.equals("video_url")
                            || key.contains("origin_video") || key.contains("source_video");
                    if (primaryKey && primary == null) {
                        primary = value;
                    }
                }
            }
        }
        if (primary == null) {
            primary = parseViduPrimaryVideoUrl(body);
        }
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (primary != null && !primary.isBlank()) {
            urls.add(primary);
        }
        if (watermark != null && !watermark.isBlank()) {
            urls.add(watermark);
        }
        return new ArrayList<>(urls);
    }

    private String parseViduTaskError(Map<String, Object> body) {
        if (body == null) {
            return "未知错误";
        }
        Object creations = body.get("creations");
        if (creations instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            String msg = firstNonBlank(
                    valueAsString(first.get("message")),
                    valueAsString(first.get("error")),
                    valueAsString(first.get("error_message")),
                    valueAsString(first.get("reason"))
            );
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        }
        return parseArkTaskError(body);
    }

    private String mergeViduMetaData(Object raw, Map<String, Object> patch) {
        Map<String, Object> out = new HashMap<>();
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    out.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        } else if (raw instanceof String text && !text.isBlank()) {
            try {
                Map<?, ?> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(text.trim(), Map.class);
                for (Map.Entry<?, ?> entry : parsed.entrySet()) {
                    if (entry.getKey() != null) {
                        out.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        out.putAll(patch);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(out);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String detectViduModelFamily(String modelName) {
        String normalized = normalize(modelName);
        if (normalized.contains("q3")) {
            return "q3";
        }
        if (normalized.contains("q2")) {
            return "q2";
        }
        if (normalized.contains("q1")) {
            return "q1";
        }
        if (normalized.contains("2.0") || normalized.contains("v2.0") || normalized.contains("vidu2")) {
            return "2.0";
        }
        return "";
    }

    private ViduMatrix defaultViduMatrix(String family) {
        return switch (normalize(family)) {
            case "q1" -> new ViduMatrix(List.of(4, 8), List.of("360p", "540p"), false);
            case "q3" -> new ViduMatrix(List.of(4, 8), List.of("540p", "720p", "1080p"), true);
            case "2.0" -> new ViduMatrix(List.of(4, 8), List.of("360p", "540p", "720p", "1080p"), true);
            default -> new ViduMatrix(List.of(4, 8), List.of("360p", "540p", "720p"), true);
        };
    }

    private List<Integer> ensureIntList(Object raw, List<Integer> defaults) {
        List<Integer> values = toIntList(raw);
        if (values.isEmpty()) {
            values = new ArrayList<>(defaults);
        }
        Collections.sort(values);
        return values;
    }

    private List<String> ensureStringList(Object raw, List<String> defaults) {
        List<String> values = toStringList(raw);
        if (values.isEmpty()) {
            values = new ArrayList<>(defaults);
        }
        return values;
    }

    private List<String> sanitizeStringList(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String value = valueAsString(item);
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        } else if (raw instanceof String text && !text.isBlank()) {
            values.add(text.trim());
        }
        return new ArrayList<>(values);
    }

    private List<Integer> toIntList(Object raw) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                Integer parsed = parseIntegerStrict(item);
                if (parsed != null) {
                    values.add(parsed);
                }
            }
        } else if (raw instanceof String text) {
            String[] split = text.split(",");
            for (String item : split) {
                Integer parsed = parseIntegerStrict(item);
                if (parsed != null) {
                    values.add(parsed);
                }
            }
        }
        return new ArrayList<>(values);
    }

    private List<String> toStringList(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String value = item == null ? "" : String.valueOf(item).trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        } else if (raw instanceof String text) {
            String[] split = text.split(",");
            for (String item : split) {
                String value = item.trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return new ArrayList<>(values);
    }

    private Integer parseIntegerStrict(Object raw) {
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            int rounded = number.intValue();
            return value == rounded ? rounded : null;
        }
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parseBooleanStrict(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof String text) {
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized)) {
                return false;
            }
        }
        return null;
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.trim().equalsIgnoreCase(target.trim())) {
                return true;
            }
        }
        return false;
    }

    private String joinInts(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> text = new ArrayList<>();
        for (Integer value : values) {
            text.add(String.valueOf(value));
        }
        return String.join(", ", text);
    }

    private Map<String, Object> sanitizeObjectMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return EMPTY_MAP;
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    @SafeVarargs
    private final List<String> firstNonEmptyStringList(List<String>... candidates) {
        if (candidates == null) {
            return List.of();
        }
        for (List<String> candidate : candidates) {
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return List.of();
    }

    private String firstItem(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String normalizeImageAdvancedCapability(String raw) {
        String capability = normalize(raw);
        return switch (capability) {
            case "vidu_reference2image", "kling_multi_reference", "outpaint", "omni" -> capability;
            default -> capability.isBlank() ? null : capability;
        };
    }

    private String inferImageAdvancedCapability(String modelName) {
        String normalized = normalize(modelName);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("outpaint") || normalized.contains("out-paint") || normalized.contains("expand")) {
            return "outpaint";
        }
        if (normalized.contains("omni")) {
            return "omni";
        }
        if ("kling-v2".equals(normalized) || normalized.contains("multi") && normalized.contains("reference")) {
            return "kling_multi_reference";
        }
        if (normalized.startsWith("vidu") || normalized.contains("reference2image")) {
            return "vidu_reference2image";
        }
        return null;
    }

    private int requireNonNegativeExpand(Integer raw, String fieldName) {
        if (raw == null) {
            return 0;
        }
        if (raw < 0) {
            throw new BizException(400, "扩图边距 " + fieldName + " 仅支持非负整数");
        }
        return raw;
    }

    private void validateOutpaintRatio(double value, String fieldName) {
        if (value < 0d || value > 2d) {
            throw new BizException(400, "扩图边距 " + fieldName + " 超出可映射范围，请控制在原图边长的 2 倍以内");
        }
    }

    private String buildKlingOmniPrompt(String prompt, String subjectPrompt, String omniMode) {
        StringBuilder builder = new StringBuilder();
        if (subjectPrompt != null && !subjectPrompt.isBlank()) {
            builder.append("主体要求：").append(subjectPrompt.trim()).append("。");
        }
        if (omniMode != null && !omniMode.isBlank()) {
            builder.append("模式：").append(omniMode.trim()).append("。");
        }
        if (prompt != null && !prompt.isBlank()) {
            builder.append(prompt.trim());
        }
        if (builder.indexOf("<<<image_1>>>") < 0) {
            builder.append(" 请参考<<<image_1>>>保持主体一致性。");
        }
        return builder.toString().trim();
    }

    private String normalizeKlingImageInput(String raw) {
        String value = valueAsString(raw);
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:image/")) {
            int comma = trimmed.indexOf(',');
            if (comma < 0 || comma + 1 >= trimmed.length()) {
                throw new BizException(400, "Kling 参考图 data:image 格式错误");
            }
            return trimmed.substring(comma + 1).trim();
        }
        return trimmed;
    }

    private ImageDimensions readImageDimensions(String ref) {
        byte[] data = readImageBytes(ref);
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new BizException(400, "输入图片无法解析");
            }
            if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BizException(400, "输入图片尺寸无效");
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            throw new BizException(400, "输入图片无法解析");
        }
    }

    private byte[] readImageBytes(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new BizException(400, "缺少输入图片");
        }
        String trimmed = ref.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:image/")) {
            int comma = trimmed.indexOf(',');
            if (comma < 0 || comma + 1 >= trimmed.length()) {
                throw new BizException(400, "输入图片 data:image 格式错误");
            }
            try {
                byte[] decoded = Base64.getDecoder().decode(trimmed.substring(comma + 1).trim());
                if (decoded.length == 0 || decoded.length > KLING_MAX_IMAGE_BYTES) {
                    throw new BizException(400, "输入图片体积需在 0-10MB 之间");
                }
                return decoded;
            } catch (IllegalArgumentException ex) {
                throw new BizException(400, "输入图片 Base64 解码失败");
            }
        }
        if (!isValidMediaUrl(trimmed)) {
            try {
                byte[] decoded = Base64.getDecoder().decode(trimmed);
                if (decoded.length == 0 || decoded.length > KLING_MAX_IMAGE_BYTES) {
                    throw new BizException(400, "输入图片体积需在 0-10MB 之间");
                }
                return decoded;
            } catch (IllegalArgumentException ex) {
                throw new BizException(400, "输入图片地址或 Base64 不合法");
            }
        }
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(trimmed))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "aigc-server/kling-image-inspector")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(400, "输入图片地址不可访问");
            }
            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > KLING_MAX_IMAGE_BYTES) {
                throw new BizException(400, "输入图片体积需在 0-10MB 之间");
            }
            return body;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(400, "输入图片地址不可访问");
        }
    }

    private record ViduMatrix(List<Integer> durations, List<String> resolutions, boolean audioSupported) {
    }

    private record ImageDimensions(int width, int height) {
    }

    private record NormalizedImageRequest(
            String capability,
            String referenceImageUrl,
            List<String> referenceImages,
            String sourceImageUrl,
            Integer outpaintTop,
            Integer outpaintRight,
            Integer outpaintBottom,
            Integer outpaintLeft,
            String omniMode,
            String omniSubjectPrompt
    ) {
        private boolean hasAdvancedRequest() {
            return capability != null && !capability.isBlank();
        }
    }

    private record NormalizedAdvancedMedia(
            NormalizedImageRequest image,
            String videoReferenceImageUrl,
            Map<String, Object> videoViduOptions
    ) {
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
                valueAsString(body.get("state")),
                nestedValue(body, "output", "status"),
                nestedValue(body, "output", "task_status"),
                nestedValue(body, "data", "status"),
                nestedValue(body, "data", "task_status"),
                nestedValue(body, "result", "status")
                ,
                nestedValue(body, "result", "task_status")
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
                valueAsString(body.get("task_status_msg")),
                valueAsString(body.get("err_code")),
                nestedValue(body, "data", "message"),
                nestedValue(body, "data", "task_status_msg"),
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
                task.getVideoModelRejectReason(),
                task.getPersistedImageFileIds() == null ? List.of() : task.getPersistedImageFileIds(),
                task.getPersistedVideoFileIds() == null ? List.of() : task.getPersistedVideoFileIds()
        );
    }

    /**
     * 将工作台生成的外链图片/视频拉取到本地 {@link WorkspaceConstants#WORKSPACE_PROJECT_ID}，便于长期访问。
     */
    private void persistWorkspaceGenerationFiles(
            String taskId,
            GenerationTask task,
            List<String> imageResults,
            List<String> videoResults
    ) {
        boolean hasImages = imageResults != null && !imageResults.isEmpty();
        boolean hasVideos = videoResults != null && !videoResults.isEmpty();
        if (!hasImages && !hasVideos) {
            task.setPersistedImageFileIds(List.of());
            task.setPersistedVideoFileIds(List.of());
            return;
        }
        try {
            ScriptProjectAggregate aggregate = scriptProjectService.require(WorkspaceConstants.WORKSPACE_PROJECT_ID);
            List<String> persistedImg = new ArrayList<>();
            List<String> displayImg = new ArrayList<>();
            if (imageResults != null) {
                for (int i = 0; i < imageResults.size(); i++) {
                    String url = imageResults.get(i);
                    if (url == null || url.isBlank()) {
                        displayImg.add("");
                        continue;
                    }
                    String trimmed = url.trim();
                    try {
                        StoredFileRecord rec = storeWorkspaceImage(taskId, i, trimmed);
                        scriptProjectService.upsertFile(aggregate, rec);
                        persistedImg.add(rec.fileId);
                        displayImg.add(localAssetFileService.toPublicUrl(rec.fileId));
                    } catch (Exception ex) {
                        displayImg.add(trimmed);
                    }
                }
            }
            List<String> persistedVid = new ArrayList<>();
            List<String> displayVid = new ArrayList<>();
            if (videoResults != null) {
                for (int i = 0; i < videoResults.size(); i++) {
                    String url = videoResults.get(i);
                    if (url == null || url.isBlank()) {
                        displayVid.add("");
                        continue;
                    }
                    String trimmed = url.trim();
                    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                        displayVid.add(trimmed);
                        continue;
                    }
                    try {
                        StoredFileRecord rec = localAssetFileService.storeRemote(
                                WorkspaceConstants.WORKSPACE_PROJECT_ID,
                                "workspace-gen/" + taskId + "/vid-" + i + ".mp4",
                                "video/mp4",
                                trimmed
                        );
                        scriptProjectService.upsertFile(aggregate, rec);
                        persistedVid.add(rec.fileId);
                        displayVid.add(localAssetFileService.toPublicUrl(rec.fileId));
                    } catch (Exception ex) {
                        displayVid.add(trimmed);
                    }
                }
            }
            scriptProjectService.save(aggregate);
            task.setPersistedImageFileIds(persistedImg);
            task.setPersistedVideoFileIds(persistedVid);
            if (!displayImg.isEmpty()) {
                task.setImageResults(displayImg);
            }
            if (!displayVid.isEmpty()) {
                task.setVideoResults(displayVid);
            }
        } catch (Exception ex) {
            task.setPersistedImageFileIds(List.of());
            task.setPersistedVideoFileIds(List.of());
        }
    }

    private StoredFileRecord storeWorkspaceImage(String taskId, int index, String urlOrBase64) {
        if (urlOrBase64.startsWith("http://") || urlOrBase64.startsWith("https://")) {
            String ext = guessImageExtension(urlOrBase64);
            String mediaType = guessImageMediaType(urlOrBase64);
            return localAssetFileService.storeRemote(
                    WorkspaceConstants.WORKSPACE_PROJECT_ID,
                    "workspace-gen/" + taskId + "/img-" + index + ext,
                    mediaType,
                    urlOrBase64
            );
        }
        return localAssetFileService.storeBase64(
                WorkspaceConstants.WORKSPACE_PROJECT_ID,
                "workspace-gen/" + taskId + "/img-" + index + ".png",
                "image/png",
                urlOrBase64
        );
    }

    private String guessImageExtension(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) {
            return ".png";
        }
        if (lower.contains(".webp")) {
            return ".webp";
        }
        if (lower.contains(".gif")) {
            return ".gif";
        }
        if (lower.contains(".jpeg") || lower.contains(".jpg")) {
            return ".jpg";
        }
        return ".png";
    }

    private String guessImageMediaType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) {
            return "image/png";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        if (lower.contains(".gif")) {
            return "image/gif";
        }
        if (lower.contains(".jpeg") || lower.contains(".jpg")) {
            return "image/jpeg";
        }
        return "image/png";
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
