package com.example.aigc.service;

import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProviderCatalog {

    private final Map<String, ProviderDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public ProviderCatalog() {
        register(new ProviderDefinition(
                "openai",
                "OpenAI",
                "https://api.openai.com",
                "openai",
                "/v1/chat/completions",
                "/v1/models",
                AuthMode.BEARER,
                true,
                "/v1/images/generations",
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        ), "gpt", "chatgpt");
        register(new ProviderDefinition(
                "onelinkai",
                "OneLinkAI",
                "https://api.onelinkai.cloud",
                "openai",
                "/v1/chat/completions",
                "/v1/models",
                AuthMode.BEARER,
                true,
                "/v1/images/generations",
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of(
                        "gpt-4o",
                        "gpt-4o-mini",
                        "claude-opus-4-6",
                        "claude-opus-4-7",
                        "claude-sonnet-4-6",
                        "deepseek-v4-flash",
                        "deepseek-v4-pro",
                        "doubao-seed-2.0-code-preview-260215",
                        "doubao-seed-2.0-lite-260215",
                        "doubao-seed-2.0-mini-260215",
                        "doubao-seed-2.0-pro-260215",
                        "gemini-3-flash-preview",
                        "gemini-3.1-pro-preview",
                        "glm-4.7-flashx",
                        "glm-5",
                        "glm-5-turbo",
                        "GLM-5.1",
                        "gpt-5.2-codex",
                        "gpt-5.3-codex",
                        "gpt-5.4",
                        "kimi-k2.5",
                        "kimi-k2.6",
                        "minimax-m2.5",
                        "minimax-m2.7",
                        "qwen3.5-35b-a3b",
                        "qwen3.5-397b-a17b",
                        "qwen3.5-flash",
                        "qwen3.5-plus",
                        "qwen3.6-plus",
                        "step-3.5-flash",
                        "gemini-2.5-pro",
                        "gemini-2.5-flash",
                        "wanx-v1",
                        "doubao-seedance-1.5-pro",
                        "doubao-seedance-2.0",
                        "kling-v1",
                        "kling-v1-6",
                        "video-kling-v3",
                        "video-kling-v3",
                        "video-kling-v3-6",
                        "MiniMax-M2.1",
                        "viduq3-turbo",
                        "video-viduq3-pro",
                        "image-viduq3-pro",
                        "image-vidu-q2",
                        "viduq2-turbo",
                        "viduq2",
                        "viduq1",
                        "viduq1-classic",
                        "vidu2.0"
                ),
                null,
                false
        ), "onelink", "onelink-ai", "一键AI");
        register(new ProviderDefinition(
                "anthropic",
                "Anthropic",
                "https://api.anthropic.com",
                "anthropic",
                "/v1/messages",
                null,
                AuthMode.X_API_KEY,
                true,
                null,
                null,
                null,
                GatewayKind.ANTHROPIC,
                List.of(
                        "claude-opus-4-6",
                        "claude-sonnet-4-6",
                        "claude-haiku-4-5-20251001",
                        "claude-3-5-sonnet-20241022",
                        "claude-3-5-haiku-20241022",
                        "claude-3-opus-20240229"
                ),
                null,
                false
        ), "claude");
        register(new ProviderDefinition(
                "deepseek",
                "DeepSeek",
                "https://api.deepseek.com",
                "openai",
                "/v1/chat/completions",
                null,
                AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of("deepseek-chat", "deepseek-reasoner", "deepseek-coder"),
                null,
                false
        ));
        register(new ProviderDefinition(
                "qwen",
                "通义千问",
                "https://dashscope.aliyuncs.com/compatible-mode",
                "openai",
                "/v1/chat/completions",
                null,
                AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of("qwen-max", "qwen-plus", "qwen-turbo", "qwen-turbo-latest", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct"),
                null,
                false
        ), "通义千问", "aliyun", "dashscope");
        register(new ProviderDefinition(
                "minimax",
                "MiniMax",
                "https://api.minimax.chat",
                "openai",
                "/v1/chat/completions",
                null,
                AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of("MiniMax-Text-01", "abab6.5s-chat", "abab6.5g-chat", "abab5.5-chat"),
                null,
                false
        ));
        register(new ProviderDefinition(
                "glm",
                "智谱AI",
                "https://open.bigmodel.cn/api/paas/v4",
                "openai",
                "/chat/completions",
                null,
                AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of("glm-4-plus", "glm-4-air", "glm-4-flash", "glm-4", "glm-3-turbo"),
                null,
                false
        ), "zhipu", "智谱", "bigmodel", "智谱ai");
        register(new ProviderDefinition(
                "kimi",
                "KIMI",
                "https://api.moonshot.cn",
                "openai",
                "/v1/chat/completions",
                null,
                AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"),
                null,
                false
        ), "moonshot", "月之暗面");
        register(new ProviderDefinition(
                "aliyun_coding",
                "阿里 Coding",
                "https://dashscope.aliyuncs.com/compatible-mode",
                "openai",
                "/v1/chat/completions",
                null,
                AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of("qwen-coder-plus", "qwen-coder-turbo", "qwen2.5-coder-32b-instruct"),
                null,
                false
        ), "coding", "aliyun_coding", "阿里coding", "coding plan");
        register(new ProviderDefinition(
                "ollama",
                "Ollama",
                "http://localhost:11434",
                "openai",
                "/v1/chat/completions",
                "/api/tags",
                AuthMode.NONE,
                true,
                null,
                null,
                null,
                GatewayKind.OLLAMA,
                List.of(),
                null,
                false
        ));
        register(new ProviderDefinition(
                "lm_studio",
                "LM Studio",
                "http://127.0.0.1:1234",
                "openai",
                "/v1/chat/completions",
                "/v1/models",
                AuthMode.NONE,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        ), "lmstudio", "lmstudioai");
        register(new ProviderDefinition(
                "azure_openai",
                "Azure OpenAI",
                "https://YOUR_RESOURCE.openai.azure.com",
                "openai",
                "/openai/deployments/{deployment}/chat/completions",
                "/openai/models",
                AuthMode.API_KEY_HEADER,
                true,
                null,
                null,
                null,
                GatewayKind.AZURE_OPENAI,
                List.of(),
                null,
                false
        ), "azure", "azureopenai");
        register(new ProviderDefinition(
                "aws_bedrock",
                "AWS Bedrock",
                "https://bedrock-runtime.us-east-1.amazonaws.com",
                "openai",
                "",
                "",
                AuthMode.NONE,
                true,
                null,
                null,
                null,
                GatewayKind.BEDROCK,
                List.of(),
                null,
                false
        ), "bedrock", "awsbedrock");
        register(new ProviderDefinition(
                "vertex_ai",
                "Vertex AI",
                "https://us-central1-aiplatform.googleapis.com",
                "openai",
                "",
                "",
                AuthMode.NONE,
                true,
                null,
                null,
                null,
                GatewayKind.VERTEX,
                List.of(),
                null,
                false
        ), "vertex", "googlevertex");
        register(new ProviderDefinition(
                "ark",
                "方舟",
                "https://ark.cn-beijing.volces.com",
                "openai",
                "/api/v3/responses",
                null,
                AuthMode.BEARER,
                true,
                "/api/v3/images/generations",
                "/api/v3/contents/generations/tasks",
                "/api/v3/contents/generations/tasks/{taskId}",
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        ), "doubao", "火山方舟", "方舟", "ark");
        register(new ProviderDefinition(
                "moark",
                "Moark",
                "https://api.moark.com",
                "openai",
                null,
                null,
                AuthMode.BEARER,
                false,
                null,
                "/v1/async/videos/image-to-video",
                "/v1/task/{taskId}",
                GatewayKind.MOARK_I2V,
                List.of("Wan2.1-I2V-14B-720P"),
                "https://moark.com",
                true
        ), "moark");
        register(new ProviderDefinition(
                "vidu",
                "Vidu",
                "https://api.vidu.cn",
                "vidu",
                null,
                null,
                AuthMode.TOKEN,
                false,
                null,
                "/ent/v2/img2video",
                "/ent/v2/tasks/{taskId}/creations",
                GatewayKind.OPENAI_COMPAT,
                List.of(
                        "viduq3-turbo",
                        "video-viduq3-pro",
                        "image-viduq3-pro",
                        "image-vidu-q2",
                        "viduq2-turbo",
                        "viduq2",
                        "viduq1",
                        "viduq1-classic",
                        "vidu2.0"
                ),
                null,
                false
        ), "viduai");
        /** Internal: Vidu image-to-video paths via OneLink proxy; base URL comes from the user's onelinkai connection. */
        register(new ProviderDefinition(
                "vidu_onelink",
                "Vidu (OneLink)",
                "https://api.onelinkai.cloud",
                "openai",
                null,
                null,
                AuthMode.BEARER,
                false,
                null,
                "/vidu/ent/v2/img2video",
                "/vidu/ent/v2/tasks/{taskId}/creations",
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        ));
        /** Kling official API. */
        register(new ProviderDefinition(
                "kling",
                "Kling",
                "https://api.klingai.com",
                "openai",
                null,
                null,
                AuthMode.BEARER,
                false,
                null,
                "/v1/videos/text2video",
                "/v1/videos/tasks/{taskId}",
                GatewayKind.OPENAI_COMPAT,
                List.of("video-kling-v3-6", "video-kling-v3", "video-kling-v3", "kling-v1-6", "kling-v1"),
                null,
                false
        ), "kling", "可灵", "klingai", "kling-ai");
        /** Internal compatibility: Kling via OneLink proxy. */
        register(new ProviderDefinition(
                "kling_onelink",
                "Kling (OneLink)",
                "https://api.onelinkai.cloud",
                "openai",
                null,
                null,
                AuthMode.BEARER,
                false,
                null,
                "/kling/v1/videos/text2video",
                "/kling/v1/videos/tasks/{taskId}",
                GatewayKind.OPENAI_COMPAT,
                List.of("video-kling-v3-6", "video-kling-v3", "video-kling-v3", "kling-v1-6", "kling-v1", "image-kling-v3", "image-kling-v3-omni"),
                null,
                false
        ), "oneLinkKling", "onelink-kling");
    }

    private void register(ProviderDefinition definition, String... extraAliases) {
        definitions.put(definition.key(), definition);
        aliases.put(normalizeAlias(definition.key()), definition.key());
        aliases.put(normalizeAlias(definition.displayName()), definition.key());
        for (String alias : extraAliases) {
            aliases.put(normalizeAlias(alias), definition.key());
        }
    }

    public ProviderDefinition require(String rawProvider) {
        String key = normalize(rawProvider);
        ProviderDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "不支持的提供商: " + rawProvider);
        }
        return definition;
    }

    public String normalize(String rawProvider) {
        if (rawProvider == null || rawProvider.isBlank()) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "提供商不能为空");
        }
        String key = aliases.get(normalizeAlias(rawProvider));
        return key == null ? rawProvider.trim().toLowerCase(Locale.ROOT) : key;
    }

    public List<ProviderDefinition> list() {
        return new ArrayList<>(definitions.values());
    }

    public String defaultBaseUrl(String rawProvider) {
        return require(rawProvider).defaultBaseUrl();
    }

    private String normalizeAlias(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
    }

    public enum AuthMode {
        BEARER,
        X_API_KEY,
        /** Azure OpenAI: {@code api-key} header. */
        API_KEY_HEADER,
        /** Vidu: {@code Authorization: Token <apiKey>}. */
        TOKEN,
        NONE
    }

    public record ProviderDefinition(
            String key,
            String displayName,
            String defaultBaseUrl,
            String apiFormat,
            String chatPath,
            String modelsPath,
            AuthMode authMode,
            boolean textProxySupported,
            String imageGenerationPath,
            String videoSubmitPath,
            String videoResultPath,
            GatewayKind gatewayKind,
            List<String> staticModels,
            /** When set, {@link ProviderHttpGateway#queryVideoTask} polls this host instead of the connection base URL. */
            String videoTaskStatusBaseUrl,
            /** Use multipart/form-data for {@link ProviderHttpGateway#submitVideoTask} (e.g. Moark I2V). */
            boolean videoSubmitMultipart
    ) {
    }
}
