package com.example.aigc.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FormatConversionService {

    public Map<String, Object> convertRequest(Map<String, Object> request, String clientFormat, String backendFormat) {
        if (clientFormat == null || backendFormat == null || clientFormat.equalsIgnoreCase(backendFormat)) {
            return request;
        }
        if ("openai".equalsIgnoreCase(clientFormat) && "anthropic".equalsIgnoreCase(backendFormat)) {
            return openaiToAnthropic(request);
        }
        if ("anthropic".equalsIgnoreCase(clientFormat) && "openai".equalsIgnoreCase(backendFormat)) {
            return anthropicToOpenai(request);
        }
        return request;
    }

    public Map<String, Object> convertResponse(Map<String, Object> response, String backendFormat, String clientFormat, String model) {
        if (clientFormat == null || backendFormat == null || clientFormat.equalsIgnoreCase(backendFormat)) {
            return response;
        }
        if ("anthropic".equalsIgnoreCase(backendFormat) && "openai".equalsIgnoreCase(clientFormat)) {
            return anthropicToOpenaiResponse(response, model);
        }
        if ("openai".equalsIgnoreCase(backendFormat) && "anthropic".equalsIgnoreCase(clientFormat)) {
            return openaiToAnthropicResponse(response);
        }
        return response;
    }

    public Map<String, Integer> extractUsage(Map<String, Object> response, String backendFormat) {
        Object usageNode = response == null ? null : response.get("usage");
        if (!(usageNode instanceof Map<?, ?> usage)) {
            return Map.of("prompt_tokens", 0, "completion_tokens", 0, "total_tokens", 0);
        }
        if ("anthropic".equalsIgnoreCase(backendFormat)) {
            int prompt = intValue(usage.get("input_tokens"));
            int completion = intValue(usage.get("output_tokens"));
            return Map.of(
                    "prompt_tokens", prompt,
                    "completion_tokens", completion,
                    "total_tokens", prompt + completion
            );
        }
        return Map.of(
                "prompt_tokens", intValue(usage.get("prompt_tokens")),
                "completion_tokens", intValue(usage.get("completion_tokens")),
                "total_tokens", intValue(usage.get("total_tokens"))
        );
    }

    private Map<String, Object> openaiToAnthropic(Map<String, Object> openaiRequest) {
        List<Map<String, Object>> convertedMessages = new ArrayList<>();
        String system = null;
        Object messagesNode = openaiRequest.get("messages");
        if (messagesNode instanceof List<?> messages) {
            for (Object item : messages) {
                if (!(item instanceof Map<?, ?> message)) {
                    continue;
                }
                String role = stringValue(message.get("role"));
                Object content = message.get("content");
                if ("system".equals(role)) {
                    system = stringValue(content);
                    continue;
                }
                if ("user".equals(role) || "assistant".equals(role)) {
                    convertedMessages.add(Map.of(
                            "role", role,
                            "content", content == null ? "" : content
                    ));
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", openaiRequest.getOrDefault("model", "claude-3-5-sonnet-20241022"));
        result.put("messages", convertedMessages);
        result.put("max_tokens", openaiRequest.getOrDefault("max_tokens", 4096));
        if (system != null && !system.isBlank()) {
            result.put("system", system);
        }
        copyIfPresent(openaiRequest, result, "temperature");
        copyIfPresent(openaiRequest, result, "stream");
        return result;
    }

    private Map<String, Object> anthropicToOpenai(Map<String, Object> anthropicRequest) {
        List<Object> messages = new ArrayList<>();
        Object system = anthropicRequest.get("system");
        if (system != null) {
            messages.add(Map.of("role", "system", "content", system));
        }
        Object rawMessages = anthropicRequest.get("messages");
        if (rawMessages instanceof List<?> messageList) {
            messages.addAll(messageList);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", anthropicRequest.getOrDefault("model", "gpt-4o"));
        result.put("messages", messages);
        copyIfPresent(anthropicRequest, result, "max_tokens");
        copyIfPresent(anthropicRequest, result, "temperature");
        copyIfPresent(anthropicRequest, result, "stream");
        return result;
    }

    private Map<String, Object> anthropicToOpenaiResponse(Map<String, Object> anthropicResponse, String model) {
        String content = "";
        Object contentNode = anthropicResponse.get("content");
        if (contentNode instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            content = stringValue(first.get("text"));
        }
        Map<String, Integer> usage = extractUsage(anthropicResponse, "anthropic");
        return Map.of(
                "id", anthropicResponse.getOrDefault("id", ""),
                "object", "chat.completion",
                "model", model == null || model.isBlank() ? anthropicResponse.getOrDefault("model", "") : model,
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", content),
                        "finish_reason", anthropicResponse.getOrDefault("stop_reason", "stop")
                )),
                "usage", usage
        );
    }

    private Map<String, Object> openaiToAnthropicResponse(Map<String, Object> openaiResponse) {
        String content = "";
        String finishReason = "end_turn";
        Object choicesNode = openaiResponse.get("choices");
        if (choicesNode instanceof List<?> choices && !choices.isEmpty() && choices.get(0) instanceof Map<?, ?> first) {
            Object messageNode = first.get("message");
            if (messageNode instanceof Map<?, ?> message) {
                content = stringValue(message.get("content"));
            }
            Object finishReasonNode = first.get("finish_reason");
            finishReason = finishReasonNode == null ? "end_turn" : stringValue(finishReasonNode);
        }
        Map<String, Integer> usage = extractUsage(openaiResponse, "openai");
        return Map.of(
                "id", openaiResponse.getOrDefault("id", ""),
                "type", "message",
                "role", "assistant",
                "model", openaiResponse.getOrDefault("model", ""),
                "content", List.of(Map.of("type", "text", "text", content)),
                "stop_reason", finishReason,
                "usage", Map.of(
                        "input_tokens", usage.getOrDefault("prompt_tokens", 0),
                        "output_tokens", usage.getOrDefault("completion_tokens", 0)
                )
        );
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }
}
