package com.example.aigcspringai.service;

import com.example.aigcspringai.dto.TextGenerationContext;
import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;
import com.example.aigcspringai.dto.TextMessage;
import com.example.aigcspringai.service.ai.TextGenerationFacade;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RouterProxyService {

    private final TextGenerationFacade textGenerationFacade;

    public RouterProxyService(TextGenerationFacade textGenerationFacade) {
        this.textGenerationFacade = textGenerationFacade;
    }

    public Map<String, Object> proxyChat(Map<String, Object> body, String format) {
        TextGenerationRequest request = toRequest(body);
        TextGenerationContext context = new TextGenerationContext("router_proxy_" + format, "default", "anonymous", UUID.randomUUID().toString(), true);
        TextGenerationResult result = textGenerationFacade.generate(request, context);
        return Map.of(
                "id", "chatcmpl-" + UUID.randomUUID(),
                "object", "chat.completion",
                "model", result.model(),
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", result.text()),
                        "finish_reason", result.finishReason()
                )),
                "usage", Map.of(
                        "prompt_tokens", result.usage().promptTokens(),
                        "completion_tokens", result.usage().completionTokens(),
                        "total_tokens", result.usage().totalTokens()
                ),
                "fallbackOccurred", result.fallbackOccurred()
        );
    }

    public List<TextGenerationStreamChunk> proxyChatStream(Map<String, Object> body, String format) {
        TextGenerationRequest request = toRequest(body);
        TextGenerationContext context = new TextGenerationContext("router_proxy_stream_" + format, "default", "anonymous", UUID.randomUUID().toString(), true);
        return textGenerationFacade.stream(request, context);
    }

    @SuppressWarnings("unchecked")
    private TextGenerationRequest toRequest(Map<String, Object> body) {
        String provider = body.getOrDefault("provider", "openai").toString();
        String model = body.getOrDefault("model", "gpt-4o-mini").toString();
        Object rawMessages = body.get("messages");
        List<TextMessage> messages = new ArrayList<>();
        if (rawMessages instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String role = map.get("role") == null ? "user" : map.get("role").toString();
                    String content = map.get("content") == null ? "" : map.get("content").toString();
                    messages.add(new TextMessage(role, content));
                }
            }
        }
        if (messages.isEmpty()) {
            messages.add(new TextMessage("user", body.getOrDefault("prompt", "hello").toString()));
        }
        boolean stream = Boolean.TRUE.equals(body.get("stream"));
        return new TextGenerationRequest(
                provider,
                model,
                messages,
                stream,
                null,
                null,
                null,
                "text",
                UUID.randomUUID().toString(),
                30_000L,
                Map.of()
        );
    }
}
