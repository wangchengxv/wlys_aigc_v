package com.example.aigc.service.impl;

import com.example.aigc.dto.ModelOptionDetailData;
import com.example.aigc.dto.ReversePromptModelOptionsData;
import com.example.aigc.dto.ReversePromptRequest;
import com.example.aigc.dto.ReversePromptResponse;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.service.ApiKeyCryptoService;
import com.example.aigc.service.ConnectionMetadataHelper;
import com.example.aigc.service.ModelCapabilityService;
import com.example.aigc.service.ProviderCatalog;
import com.example.aigc.service.ProviderGatewayException;
import com.example.aigc.service.ProviderHttpGateway;
import com.example.aigc.service.ReversePromptService;
import com.example.aigc.service.RouterRoutingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReversePromptServiceImpl implements ReversePromptService {
    private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Set<String> DOUBAO_MODEL_MARKERS = Set.of("doubao", "seedream");

    private final ModelConfigRepository modelConfigRepository;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final ModelCapabilityService modelCapabilityService;
    private final ProviderCatalog providerCatalog;
    private final ProviderHttpGateway providerHttpGateway;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final RouterRoutingService routerRoutingService;
    private final ObjectMapper objectMapper;

    public ReversePromptServiceImpl(
            ModelConfigRepository modelConfigRepository,
            ConnectionConfigRepository connectionConfigRepository,
            ModelCapabilityService modelCapabilityService,
            ProviderCatalog providerCatalog,
            ProviderHttpGateway providerHttpGateway,
            ApiKeyCryptoService apiKeyCryptoService,
            RouterRoutingService routerRoutingService,
            ObjectMapper objectMapper
    ) {
        this.modelConfigRepository = modelConfigRepository;
        this.connectionConfigRepository = connectionConfigRepository;
        this.modelCapabilityService = modelCapabilityService;
        this.providerCatalog = providerCatalog;
        this.providerHttpGateway = providerHttpGateway;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.routerRoutingService = routerRoutingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReversePromptModelOptionsData listModels() {
        List<ResolvedCandidate> candidates = listCandidates();
        if (candidates.isEmpty()) {
            return new ReversePromptModelOptionsData(null, List.of(), List.of());
        }
        List<String> options = candidates.stream()
                .map(item -> item.model().getModelName())
                .distinct()
                .toList();
        List<ModelOptionDetailData> details = candidates.stream()
                .map(item -> new ModelOptionDetailData(
                        item.model().getModelName(),
                        item.model().getName(),
                        item.provider().displayName(),
                        "text",
                        item.model().isEnabled(),
                        item.connection().isEnabled()
                ))
                .toList();
        return new ReversePromptModelOptionsData(resolveDefaultModel(candidates), options, details);
    }

    @Override
    public ReversePromptResponse reversePrompt(ReversePromptRequest request, String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new BizException(401, "缺少用户标识");
        }
        String imageInput = normalizeAndValidateImage(request.image());
        List<ResolvedCandidate> candidates = listCandidates();
        if (candidates.isEmpty()) {
            throw new BizException(400, "未找到可用豆包模型，请先在模型配置中启用");
        }
        ResolvedCandidate selected = pickCandidate(candidates, request.model());
        if (selected == null) {
            throw new BizException(400, "指定模型不在可用豆包模型列表中");
        }

        Map<String, Object> payload = buildChatPayload(selected.model().getModelName(), imageInput);
        String apiKey = resolveApiKey(selected.connection(), selected.provider());
        Map<String, Object> metadata = ConnectionMetadataHelper.decryptForUse(selected.connection().getMetadata(), apiKeyCryptoService);
        Map<String, Object> gatewayResponse;
        try {
            gatewayResponse = providerHttpGateway.invokeChat(
                    selected.provider(),
                    selected.connection().getBaseUrl(),
                    apiKey,
                    metadata,
                    payload,
                    Duration.ofSeconds(Math.max(15, routerRoutingService.timeoutSeconds()))
            );
        } catch (ProviderGatewayException ex) {
            throw new BizException(502, ex.getMessage());
        }

        String content = parseAssistantContent(gatewayResponse, selected.provider().apiFormat());
        return parseReversePromptResponse(selected.model().getModelName(), content);
    }

    private List<ResolvedCandidate> listCandidates() {
        List<ResolvedCandidate> out = new ArrayList<>();
        for (ModelConfig model : modelConfigRepository.findAll()) {
            if (model == null || !model.isEnabled()) {
                continue;
            }
            if (!matchesDoubaoFamily(model.getModelName())) {
                continue;
            }
            ConnectionConfig connection = connectionConfigRepository.findById(model.getConnectionId()).orElse(null);
            if (connection == null || !connection.isEnabled()) {
                continue;
            }
            ProviderCatalog.ProviderDefinition provider = providerCatalog.require(connection.getProvider());
            if (!provider.textProxySupported() || provider.chatPath() == null || provider.chatPath().isBlank()) {
                continue;
            }
            if (!modelCapabilityService.supports(model, "text")) {
                continue;
            }
            out.add(new ResolvedCandidate(model, connection, provider));
        }
        return out;
    }

    private String resolveDefaultModel(List<ResolvedCandidate> candidates) {
        List<String> orderedConnectionIds = routerRoutingService.resolveOrderedConnections(true).stream()
                .map(ConnectionConfig::getId)
                .toList();
        for (String connectionId : orderedConnectionIds) {
            for (ResolvedCandidate candidate : candidates) {
                if (connectionId.equals(candidate.connection().getId())) {
                    return candidate.model().getModelName();
                }
            }
        }
        return candidates.get(0).model().getModelName();
    }

    private ResolvedCandidate pickCandidate(List<ResolvedCandidate> candidates, String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            String defaultModel = resolveDefaultModel(candidates);
            for (ResolvedCandidate candidate : candidates) {
                if (normalize(candidate.model().getModelName()).equals(normalize(defaultModel))) {
                    return candidate;
                }
            }
            return candidates.get(0);
        }
        String expected = normalize(requestedModel);
        for (ResolvedCandidate candidate : candidates) {
            if (expected.equals(normalize(candidate.model().getModelName()))) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeAndValidateImage(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            throw new BizException(400, "图片不能为空");
        }
        String low = value.toLowerCase(Locale.ROOT);
        if (low.startsWith("http://") || low.startsWith("https://")) {
            if (value.length() > 2048) {
                throw new BizException(400, "图片 URL 过长");
            }
            return value;
        }
        if (!low.startsWith("data:image/") || !low.contains("base64,")) {
            throw new BizException(400, "图片仅支持 http(s) URL 或 data:image base64");
        }
        int comma = value.indexOf(',');
        if (comma <= 0 || comma + 1 >= value.length()) {
            throw new BizException(400, "data:image 格式错误");
        }
        String payload = value.substring(comma + 1).trim();
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new BizException(400, "Base64 图片解码失败");
        }
        if (decoded.length == 0 || decoded.length > MAX_IMAGE_BYTES) {
            throw new BizException(400, "图片体积需在 0-20MB 之间");
        }
        return value;
    }

    private Map<String, Object> buildChatPayload(String modelName, String imageInput) {
        String system = """
                你是专业的视觉提示词反推助手。
                请根据用户图片，输出严格 JSON（不要 markdown 代码块，不要额外解释）：
                {
                  "positivePrompt": "正向提示词",
                  "negativePrompt": "反向提示词",
                  "style": "风格",
                  "lighting": "光线",
                  "composition": "构图",
                  "camera": "镜头",
                  "colorTone": "色彩倾向",
                  "parameters": {
                    "aspectRatio": "",
                    "lens": "",
                    "quality": "",
                    "stylize": ""
                  }
                }
                字段缺失时请返回空字符串，不要省略字段。
                """;
        Map<String, Object> imageUrl = new LinkedHashMap<>();
        imageUrl.put("url", imageInput);
        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type", "text",
                "text", "请反推该图片的提示词，并按约定 JSON 返回。"
        ));
        userContent.add(Map.of(
                "type", "image_url",
                "image_url", imageUrl
        ));
        return Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.2,
                "max_tokens", 1200
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
                if (content instanceof String text) {
                    return text;
                }
                if (content instanceof List<?> contentList && !contentList.isEmpty()) {
                    for (Object item : contentList) {
                        if (item instanceof Map<?, ?> itemMap && itemMap.get("text") != null) {
                            return String.valueOf(itemMap.get("text"));
                        }
                    }
                }
            }
        }
        return "";
    }

    private ReversePromptResponse parseReversePromptResponse(String modelName, String content) {
        String rawText = content == null ? "" : content.trim();
        if (rawText.isBlank()) {
            throw new BizException(502, "模型未返回可用内容");
        }
        Map<String, Object> parsed = tryParseJson(rawText);
        if (parsed.isEmpty()) {
            return new ReversePromptResponse(
                    modelName,
                    rawText,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    Map.of(),
                    rawText
            );
        }
        return new ReversePromptResponse(
                modelName,
                firstText(parsed, "positivePrompt", "positive_prompt", "positive", "prompt"),
                firstText(parsed, "negativePrompt", "negative_prompt", "negative"),
                firstText(parsed, "style"),
                firstText(parsed, "lighting"),
                firstText(parsed, "composition"),
                firstText(parsed, "camera"),
                firstText(parsed, "colorTone", "color_tone", "color"),
                stringifyMap(parsed.get("parameters")),
                rawText
        );
    }

    private Map<String, Object> tryParseJson(String rawText) {
        String text = rawText.trim();
        try {
            return objectMapper.readValue(text, MAP_TYPE);
        } catch (Exception ignored) {
        }
        if (text.startsWith("```")) {
            int firstLineBreak = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineBreak > 0 && lastFence > firstLineBreak) {
                String inner = text.substring(firstLineBreak + 1, lastFence).trim();
                try {
                    return objectMapper.readValue(inner, MAP_TYPE);
                } catch (Exception ignored) {
                }
            }
        }
        return Map.of();
    }

    private Map<String, String> stringifyMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            out.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return out;
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String resolveApiKey(ConnectionConfig connection, ProviderCatalog.ProviderDefinition provider) {
        if (provider.authMode() == ProviderCatalog.AuthMode.NONE) {
            return "";
        }
        String encrypted = connection.getEncryptedApiKey();
        if (encrypted == null || encrypted.isBlank()) {
            throw new BizException(400, "连接未配置密钥：" + connection.getName());
        }
        return apiKeyCryptoService.decrypt(encrypted);
    }

    private boolean matchesDoubaoFamily(String modelName) {
        String normalized = normalize(modelName);
        if (normalized.isBlank()) {
            return false;
        }
        for (String marker : DOUBAO_MODEL_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ResolvedCandidate(
            ModelConfig model,
            ConnectionConfig connection,
            ProviderCatalog.ProviderDefinition provider
    ) {
    }
}
