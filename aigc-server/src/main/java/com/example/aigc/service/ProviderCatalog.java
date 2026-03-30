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
                List.of()
        ), "gpt", "chatgpt");
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
                )
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
                List.of("deepseek-chat", "deepseek-reasoner", "deepseek-coder")
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
                List.of("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct")
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
                List.of("MiniMax-Text-01", "abab6.5s-chat", "abab6.5g-chat", "abab5.5-chat")
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
                List.of("glm-4-plus", "glm-4-air", "glm-4-flash", "glm-4", "glm-3-turbo")
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
                List.of("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
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
                List.of("qwen-coder-plus", "qwen-coder-turbo", "qwen2.5-coder-32b-instruct")
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
                List.of()
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
                List.of()
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
                List.of()
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
                List.of()
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
                List.of()
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
                List.of()
        ), "doubao", "火山方舟", "方舟", "ark");
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
            List<String> staticModels
    ) {
    }
}
