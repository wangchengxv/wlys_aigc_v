package com.example.aigc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vertex AI Gemini via generateContent (regional endpoint).
 */
@Service
public class VertexAiGatewayService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public VertexAiGatewayService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<String> defaultGeminiModels() {
        return List.of(
                "gemini-2.0-flash-001",
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-1.0-pro"
        );
    }

    public Map<String, Object> generateContent(
            String projectId,
            String location,
            String serviceAccountJson,
            Map<String, Object> openAiPayload
    ) {
        Object modelObj = openAiPayload.get("model");
        String modelId = modelObj == null ? "" : String.valueOf(modelObj).trim();
        if (modelId.isEmpty()) {
            throw new ProviderGatewayException(400, "缺少 model");
        }
        String userText = extractUserText(openAiPayload);
        if (userText.isBlank()) {
            throw new ProviderGatewayException(400, "缺少用户消息");
        }
        String token = bearerToken(serviceAccountJson);
        String host = location + "-aiplatform.googleapis.com";
        String path = "/v1/projects/" + projectId + "/locations/" + location
                + "/publishers/google/models/" + modelId + ":generateContent";
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userText))
                        )
                )
        );
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + path))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ProviderGatewayException(response.statusCode(), extractErr(response.body()));
            }
            Map<String, Object> root = objectMapper.readValue(response.body(), MAP_TYPE);
            String text = extractGeminiText(root);
            return openAiShaped(text);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "Vertex AI 调用失败: " + ex.getMessage());
        }
    }

    private String bearerToken(String serviceAccountJson) {
        try {
            GoogleCredentials creds = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
            creds.refreshIfExpired();
            return creds.getAccessToken().getTokenValue();
        } catch (Exception ex) {
            throw new ProviderGatewayException(400, "Service Account JSON 无效: " + ex.getMessage());
        }
    }

    private String extractUserText(Map<String, Object> openAiPayload) {
        Object raw = openAiPayload.get("messages");
        if (!(raw instanceof List<?> list)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String role = String.valueOf(m.get("role")).toLowerCase();
            if ("system".equals(role) || "user".equals(role)) {
                Object c = m.get("content");
                if (c instanceof String) {
                    sb.append((String) c).append('\n');
                }
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> root) {
        Object candidates = root.get("candidates");
        if (!(candidates instanceof List<?> list) || list.isEmpty()) {
            return "";
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> cMap)) {
            return "";
        }
        Object content = cMap.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return "";
        }
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partsList)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object p : partsList) {
            if (p instanceof Map<?, ?> pm && pm.get("text") != null) {
                sb.append(pm.get("text"));
            }
        }
        return sb.toString();
    }

    private Map<String, Object> openAiShaped(String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", text);
        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);
        choice.put("index", 0);
        return Map.of("choices", List.of(choice));
    }

    private String extractErr(String body) {
        try {
            Map<String, Object> m = objectMapper.readValue(body, MAP_TYPE);
            Object err = m.get("error");
            if (err instanceof Map<?, ?> em && em.get("message") != null) {
                return String.valueOf(em.get("message"));
            }
        } catch (Exception ignored) {
        }
        return body == null ? "error" : body.substring(0, Math.min(body.length(), 400));
    }
}
