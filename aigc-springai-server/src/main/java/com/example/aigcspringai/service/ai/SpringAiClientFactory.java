package com.example.aigcspringai.service.ai;

import com.example.aigcspringai.config.AiGatewayProperties;
import com.example.aigcspringai.exception.ClientBuildException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpringAiClientFactory {

    private final ChatClient.Builder chatClientBuilder;
    private final AiGatewayProperties aiGatewayProperties;
    private final Map<String, AiTextClient> cache = new ConcurrentHashMap<>();

    public SpringAiClientFactory(
            ChatClient.Builder chatClientBuilder,
            AiGatewayProperties aiGatewayProperties
    ) {
        this.chatClientBuilder = chatClientBuilder;
        this.aiGatewayProperties = aiGatewayProperties;
    }

    public AiTextClient getOrCreate(ClientBuildSpec spec) {
        AiGatewayProperties.Provider provider = aiGatewayProperties.getProviders().get(spec.providerCode());
        if (provider == null) {
            throw new ClientBuildException("未找到 provider 配置: " + spec.providerCode());
        }

        String cacheKey = spec.connectionId() + ":" + spec.modelCode() + ":" + authFingerprint(provider.getApiKey());
        return cache.computeIfAbsent(cacheKey, key -> build(spec, provider));
    }

    private AiTextClient build(ClientBuildSpec spec, AiGatewayProperties.Provider provider) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            return new SpringAiTextClient(chatClient, spec.providerCode(), spec.modelCode());
        } catch (Exception ex) {
            throw new ClientBuildException("SpringAI 客户端创建失败: " + ex.getMessage(), ex);
        }
    }

    public void invalidate(String connectionId, String modelCode, String apiKey) {
        String prefix = connectionId + ":" + modelCode + ":" + authFingerprint(apiKey);
        cache.remove(prefix);
    }

    private String authFingerprint(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "empty";
        }
        int length = apiKey.length();
        return "len-" + length;
    }

    public record ClientBuildSpec(
            String connectionId,
            String providerCode,
            String modelCode,
            long timeoutMs,
            Map<String, Object> metadata
    ) {
    }
}
