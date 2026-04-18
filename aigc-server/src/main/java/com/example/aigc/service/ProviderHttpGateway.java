package com.example.aigc.service;

import com.example.aigc.service.ProviderCatalog.AuthMode;
import com.example.aigc.service.ProviderCatalog.ProviderDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProviderHttpGateway {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> HEADER_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final BedrockGatewayService bedrockGatewayService;
    private final VertexAiGatewayService vertexAiGatewayService;

    public ProviderHttpGateway(
            ObjectMapper objectMapper,
            BedrockGatewayService bedrockGatewayService,
            VertexAiGatewayService vertexAiGatewayService
    ) {
        this.objectMapper = objectMapper;
        this.bedrockGatewayService = bedrockGatewayService;
        this.vertexAiGatewayService = vertexAiGatewayService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Map<String, Object> invokeChat(ProviderDefinition definition, String baseUrl, String apiKey, Map<String, Object> payload, Duration timeout) {
        return invokeChat(definition, baseUrl, apiKey, null, payload, timeout);
    }

    public Map<String, Object> invokeChat(
            ProviderDefinition definition,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            Duration timeout
    ) {
        Map<String, Object> meta = connectionMetadata == null ? Map.of() : connectionMetadata;
        if (definition.gatewayKind() == GatewayKind.BEDROCK) {
            String region = stringVal(meta.get(ConnectionMetadataHelper.AWS_REGION));
            String accessKeyId = stringVal(meta.get(ConnectionMetadataHelper.AWS_ACCESS_KEY_ID));
            String sessionToken = stringVal(meta.get(ConnectionMetadataHelper.AWS_SESSION_TOKEN));
            return bedrockGatewayService.converse(region, accessKeyId, apiKey, sessionToken, payload);
        }
        if (definition.gatewayKind() == GatewayKind.VERTEX) {
            String project = stringVal(meta.get(ConnectionMetadataHelper.VERTEX_PROJECT));
            String location = stringVal(meta.get(ConnectionMetadataHelper.VERTEX_LOCATION));
            String saJson = stringVal(meta.get(ConnectionMetadataHelper.VERTEX_SA_JSON));
            return vertexAiGatewayService.generateContent(project, location, saJson, payload);
        }
        if (definition.gatewayKind() == GatewayKind.AZURE_OPENAI) {
            return azureChat(baseUrl, apiKey, meta, payload, timeout);
        }
        return postJson(baseUrl, definition.chatPath(), definition, apiKey, meta, payload, timeout);
    }

    public InputStream streamChat(ProviderDefinition definition, String baseUrl, String apiKey, Map<String, Object> payload, Duration timeout) {
        return streamChat(definition, baseUrl, apiKey, null, payload, timeout);
    }

    public InputStream streamChat(
            ProviderDefinition definition,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            Duration timeout
    ) {
        if (definition.gatewayKind() == GatewayKind.BEDROCK || definition.gatewayKind() == GatewayKind.VERTEX) {
            Map<String, Object> resp = invokeChat(definition, baseUrl, apiKey, connectionMetadata, payload, timeout);
            try {
                String sse = "data: " + objectMapper.writeValueAsString(resp) + "\n\ndata: [DONE]\n\n";
                return new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throw new ProviderGatewayException(502, "流式封装失败");
            }
        }
        if (definition.gatewayKind() == GatewayKind.AZURE_OPENAI) {
            return azureStreamChat(baseUrl, apiKey, connectionMetadata == null ? Map.of() : connectionMetadata, payload, timeout);
        }
        if (definition.chatPath() == null || definition.chatPath().isBlank()) {
            throw new ProviderGatewayException(400, "当前提供商不支持文本代理");
        }
        try {
            HttpRequest request = buildJsonRequest(baseUrl, definition.chatPath(), definition, apiKey, connectionMetadata, payload, timeout).build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                try (InputStream errorStream = response.body()) {
                    String body = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    throw new ProviderGatewayException(response.statusCode(), extractMessage(body));
                }
            }
            return response.body();
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "模型服务流式调用失败");
        }
    }

    public List<String> listModels(ProviderDefinition definition, String baseUrl, String apiKey, Duration timeout) {
        return listModels(definition, baseUrl, apiKey, null, timeout);
    }

    public List<String> listModels(
            ProviderDefinition definition,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Duration timeout
    ) {
        Map<String, Object> meta = connectionMetadata == null ? Map.of() : connectionMetadata;
        List<String> staticModels = definition.staticModels() == null ? List.of() : definition.staticModels();
        boolean oneLinkAiHybrid = "onelinkai".equalsIgnoreCase(definition.key());
        if (definition.gatewayKind() == GatewayKind.BEDROCK) {
            String region = stringVal(meta.get(ConnectionMetadataHelper.AWS_REGION));
            String accessKeyId = stringVal(meta.get(ConnectionMetadataHelper.AWS_ACCESS_KEY_ID));
            String sessionToken = stringVal(meta.get(ConnectionMetadataHelper.AWS_SESSION_TOKEN));
            if (region.isEmpty() || accessKeyId.isEmpty() || apiKey.isEmpty()) {
                return List.of();
            }
            return bedrockGatewayService.listFoundationModelIds(region, accessKeyId, apiKey, sessionToken);
        }
        if (definition.gatewayKind() == GatewayKind.MOARK_I2V) {
            return definition.staticModels() == null || definition.staticModels().isEmpty()
                    ? List.of()
                    : List.copyOf(definition.staticModels());
        }
        if (definition.gatewayKind() == GatewayKind.VERTEX) {
            return vertexAiGatewayService.defaultGeminiModels();
        }
        if (definition.gatewayKind() == GatewayKind.AZURE_OPENAI) {
            String v = azureApiVersion(meta);
            String path = "/openai/models?api-version=" + URLEncoder.encode(v, StandardCharsets.UTF_8);
            Map<String, Object> response = getJsonAzure(baseUrl, path, apiKey, meta, timeout);
            return parseOpenAiModelIds(response);
        }
        if (!oneLinkAiHybrid && !staticModels.isEmpty()) {
            return staticModels;
        }
        if (definition.modelsPath() == null || definition.modelsPath().isBlank()) {
            return staticModels;
        }
        try {
            Map<String, Object> response = getJson(baseUrl, definition.modelsPath(), definition, apiKey, meta, timeout);
            List<String> remoteModels;
            if ("ollama".equals(definition.key())) {
                Object modelsNode = response.get("models");
                if (!(modelsNode instanceof List<?> models)) {
                    remoteModels = List.of();
                } else {
                    List<String> result = new ArrayList<>();
                    for (Object model : models) {
                        if (model instanceof Map<?, ?> map && map.get("name") != null) {
                            result.add(String.valueOf(map.get("name")));
                        }
                    }
                    remoteModels = result;
                }
            }
            else {
                remoteModels = parseOpenAiModelIds(response);
            }
            if (oneLinkAiHybrid) {
                return mergeUnique(staticModels, remoteModels);
            }
            return remoteModels;
        } catch (ProviderGatewayException ex) {
            if (oneLinkAiHybrid && !staticModels.isEmpty()) {
                return staticModels;
            }
            throw ex;
        }
    }

    private static List<String> mergeUnique(List<String> first, List<String> second) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (first != null) {
            set.addAll(first);
        }
        if (second != null) {
            set.addAll(second);
        }
        return List.copyOf(set);
    }

    private static List<String> parseOpenAiModelIds(Map<String, Object> response) {
        Object dataNode = response.get("data");
        if (!(dataNode instanceof List<?> models)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object model : models) {
            if (model instanceof Map<?, ?> map && map.get("id") != null) {
                result.add(String.valueOf(map.get("id")));
            }
        }
        return result;
    }

    private Map<String, Object> azureChat(String baseUrl, String apiKey, Map<String, Object> meta, Map<String, Object> payload, Duration timeout) {
        String v = azureApiVersion(meta);
        Object modelObj = payload.get("model");
        String deployment = modelObj == null ? "" : String.valueOf(modelObj).trim();
        if (deployment.isEmpty()) {
            throw new ProviderGatewayException(400, "Azure OpenAI 请求缺少 model（部署名）");
        }
        String path = "/openai/deployments/" + urlEncodePath(deployment) + "/chat/completions?api-version=" + URLEncoder.encode(v, StandardCharsets.UTF_8);
        Map<String, Object> body = new HashMap<>(payload);
        body.put("model", deployment);
        return postJsonAzure(baseUrl, path, apiKey, meta, body, timeout);
    }

    private InputStream azureStreamChat(String baseUrl, String apiKey, Map<String, Object> meta, Map<String, Object> payload, Duration timeout) {
        String v = azureApiVersion(meta);
        Object modelObj = payload.get("model");
        String deployment = modelObj == null ? "" : String.valueOf(modelObj).trim();
        if (deployment.isEmpty()) {
            throw new ProviderGatewayException(400, "Azure OpenAI 请求缺少 model（部署名）");
        }
        String path = "/openai/deployments/" + urlEncodePath(deployment) + "/chat/completions?api-version=" + URLEncoder.encode(v, StandardCharsets.UTF_8);
        Map<String, Object> body = new HashMap<>(payload);
        body.put("model", deployment);
        body.put("stream", true);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri(baseUrl, path, meta))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            applyCustomHeadersFromMeta(builder, meta);
            HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                try (InputStream errorStream = response.body()) {
                    String errBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    throw new ProviderGatewayException(response.statusCode(), extractMessage(errBody));
                }
            }
            return response.body();
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "Azure OpenAI 流式调用失败");
        }
    }

    private Map<String, Object> postJsonAzure(String baseUrl, String pathWithQuery, String apiKey, Map<String, Object> meta, Map<String, Object> payload, Duration timeout) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(buildUri(baseUrl, pathWithQuery, meta))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            applyCustomHeadersFromMeta(b, meta);
            HttpRequest request = b.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ProviderGatewayException(response.statusCode(), extractMessage(response.body()));
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ProviderGatewayException(502, "模型服务返回了无法解析的数据");
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "模型服务调用失败");
        }
    }

    private Map<String, Object> getJsonAzure(String baseUrl, String pathWithQuery, String apiKey, Map<String, Object> meta, Duration timeout) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(buildUri(baseUrl, pathWithQuery, meta))
                    .timeout(timeout)
                    .header("api-key", apiKey)
                    .GET();
            applyCustomHeadersFromMeta(b, meta);
            HttpRequest request = b.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ProviderGatewayException(response.statusCode(), extractMessage(response.body()));
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "列出模型失败");
        }
    }

    private static String azureApiVersion(Map<String, Object> meta) {
        Object v = meta == null ? null : meta.get(ConnectionMetadataHelper.AZURE_API_VERSION);
        if (v == null || String.valueOf(v).isBlank()) {
            return ConnectionMetadataHelper.AZURE_DEFAULT_API_VERSION;
        }
        return String.valueOf(v).trim();
    }

    private static String urlEncodePath(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stringVal(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    public Map<String, Object> generateImage(ProviderDefinition definition, String baseUrl, String apiKey, Map<String, Object> payload, Duration timeout) {
        if (definition.imageGenerationPath() == null || definition.imageGenerationPath().isBlank()) {
            throw new ProviderGatewayException(400, "当前提供商未配置图片接口");
        }
        return postJson(baseUrl, definition.imageGenerationPath(), definition, apiKey, null, payload, timeout);
    }

    public Map<String, Object> submitVideoTask(ProviderDefinition definition, String baseUrl, String apiKey, Map<String, Object> payload, Duration timeout) {
        return submitVideoTask(definition, baseUrl, apiKey, null, payload, timeout);
    }

    public Map<String, Object> submitVideoTask(
            ProviderDefinition definition,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            Duration timeout
    ) {
        if (definition.videoSubmitPath() == null || definition.videoSubmitPath().isBlank()) {
            throw new ProviderGatewayException(400, "当前提供商未配置视频接口");
        }
        if (definition.videoSubmitMultipart()) {
            return submitVideoMultipartTask(definition, baseUrl, apiKey, connectionMetadata, payload, timeout);
        }
        return postJson(baseUrl, definition.videoSubmitPath(), definition, apiKey, connectionMetadata, payload, timeout);
    }

    public Map<String, Object> queryVideoTask(ProviderDefinition definition, String baseUrl, String apiKey, String taskId, Duration timeout) {
        if (definition.videoResultPath() == null || definition.videoResultPath().isBlank()) {
            throw new ProviderGatewayException(400, "当前提供商未配置视频查询接口");
        }
        String path = definition.videoResultPath().replace("{taskId}", URLEncoder.encode(taskId, StandardCharsets.UTF_8));
        String statusBase = resolveVideoTaskStatusBase(definition, baseUrl);
        return getJson(statusBase, path, definition, apiKey, null, timeout);
    }

    private String resolveVideoTaskStatusBase(ProviderDefinition definition, String connectionBaseUrl) {
        String override = definition.videoTaskStatusBaseUrl();
        if (override != null && !override.isBlank()) {
            return trimTrailingSlash(override);
        }
        return connectionBaseUrl;
    }

    private Map<String, Object> submitVideoMultipartTask(
            ProviderDefinition definition,
            String baseUrl,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            Duration timeout
    ) {
        if (payload == null) {
            throw new ProviderGatewayException(400, "视频请求体为空");
        }
        Object imageObj = payload.get("image");
        if (imageObj == null) {
            throw new ProviderGatewayException(400, "图生视频缺少 image 参考图（HTTP/HTTPS URL）");
        }
        String imageSpec = String.valueOf(imageObj).trim();
        if (!imageSpec.startsWith("http://") && !imageSpec.startsWith("https://")) {
            throw new ProviderGatewayException(400, "图生视频 image 须为 http(s) 图片地址");
        }
        byte[] imageBytes;
        String imageMime;
        String imageFilename;
        try {
            HttpRequest.Builder imgReq = HttpRequest.newBuilder(URI.create(imageSpec))
                    .timeout(Duration.ofSeconds(30))
                    .GET();
            HttpResponse<byte[]> imgResp = httpClient.send(imgReq.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (imgResp.statusCode() >= 400) {
                throw new ProviderGatewayException(502, "下载参考图失败，HTTP " + imgResp.statusCode());
            }
            imageBytes = imgResp.body();
            imageMime = imgResp.headers().firstValue("Content-Type")
                    .map(s -> s.split(";")[0].trim())
                    .filter(s -> !s.isBlank())
                    .orElse("application/octet-stream");
            imageFilename = filenameFromUrl(imageSpec);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "下载参考图失败");
        }

        String prompt = String.valueOf(payload.getOrDefault("prompt", ""));
        String model = String.valueOf(payload.getOrDefault("model", ""));
        String steps = String.valueOf(payload.getOrDefault("num_inference_steps", "50"));
        String frames = String.valueOf(payload.getOrDefault("num_frames", "81"));
        String boundary = "----AigcFormBoundary" + UUID.randomUUID();
        String lineEnd = "\r\n";
        try {
            java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
            for (String[] part : new String[][] {
                    {"prompt", prompt},
                    {"model", model},
                    {"num_inference_steps", steps},
                    {"num_frames", frames}
            }) {
                raw.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
                raw.write(("Content-Disposition: form-data; name=\"" + part[0] + "\"" + lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
                raw.write(part[1].getBytes(StandardCharsets.UTF_8));
                raw.write(lineEnd.getBytes(StandardCharsets.UTF_8));
            }
            raw.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
            raw.write(("Content-Disposition: form-data; name=\"image\"; filename=\"" + imageFilename + "\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
            raw.write(("Content-Type: " + imageMime + lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
            raw.write(imageBytes);
            raw.write(lineEnd.getBytes(StandardCharsets.UTF_8));
            raw.write(("--" + boundary + "--" + lineEnd).getBytes(StandardCharsets.UTF_8));
            byte[] body = raw.toByteArray();
            String contentType = "multipart/form-data; boundary=" + boundary;

            HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri(baseUrl, definition.videoSubmitPath(), connectionMetadata))
                    .timeout(timeout)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            applyHeaders(builder, definition, apiKey, connectionMetadata);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ProviderGatewayException(response.statusCode(), extractMessage(response.body()));
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ProviderGatewayException(502, "模型服务返回了无法解析的数据");
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "模型服务调用失败");
        }
    }

    private static String filenameFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return "image.jpg";
            }
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            return name.isBlank() ? "image.jpg" : name;
        } catch (Exception ex) {
            return "image.jpg";
        }
    }

    public Map<String, Object> postJson(String baseUrl, String path, ProviderDefinition definition, String apiKey, Map<String, Object> payload, Duration timeout) {
        return postJson(baseUrl, path, definition, apiKey, null, payload, timeout);
    }

    public Map<String, Object> postJson(
            String baseUrl,
            String path,
            ProviderDefinition definition,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            Duration timeout
    ) {
        try {
            HttpRequest request = buildJsonRequest(baseUrl, path, definition, apiKey, connectionMetadata, payload, timeout).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ProviderGatewayException(response.statusCode(), extractMessage(response.body()));
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ProviderGatewayException(502, "模型服务返回了无法解析的数据");
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "模型服务调用失败");
        }
    }

    public Map<String, Object> getJson(String baseUrl, String path, ProviderDefinition definition, String apiKey, Duration timeout) {
        return getJson(baseUrl, path, definition, apiKey, null, timeout);
    }

    public Map<String, Object> getJson(
            String baseUrl,
            String path,
            ProviderDefinition definition,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Duration timeout
    ) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri(baseUrl, path, connectionMetadata))
                    .timeout(timeout)
                    .GET();
            applyHeaders(builder, definition, apiKey, connectionMetadata);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ProviderGatewayException(response.statusCode(), extractMessage(response.body()));
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ProviderGatewayException(502, "模型服务返回了无法解析的数据");
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "模型服务调用失败");
        }
    }

    private HttpRequest.Builder buildJsonRequest(
            String baseUrl,
            String path,
            ProviderDefinition definition,
            String apiKey,
            Map<String, Object> connectionMetadata,
            Map<String, Object> payload,
            Duration timeout
    ) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri(baseUrl, path, connectionMetadata))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
        applyHeaders(builder, definition, apiKey, connectionMetadata);
        return builder;
    }

    private void applyHeaders(HttpRequest.Builder builder, ProviderDefinition definition, String apiKey, Map<String, Object> meta) {
        if (definition.authMode() == AuthMode.BEARER && apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        if (definition.authMode() == AuthMode.X_API_KEY && apiKey != null && !apiKey.isBlank()) {
            builder.header("x-api-key", apiKey);
            builder.header("anthropic-version", "2023-06-01");
        }
        if (definition.authMode() == AuthMode.API_KEY_HEADER && apiKey != null && !apiKey.isBlank()) {
            builder.header("api-key", apiKey);
        }
        if (definition.authMode() == AuthMode.TOKEN && apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Token " + apiKey);
        }
        applyCustomHeadersFromMeta(builder, meta);
    }

    private void applyCustomHeadersFromMeta(HttpRequest.Builder builder, Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return;
        }
        String raw = stringVal(meta.get(ConnectionMetadataHelper.CUSTOM_HEADERS_JSON));
        if (raw.isBlank()) {
            return;
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(raw, HEADER_LIST_TYPE);
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                Object n = row.get("name");
                Object v = row.get("value");
                if (n != null && !String.valueOf(n).isBlank()) {
                    builder.header(String.valueOf(n).trim(), v == null ? "" : String.valueOf(v));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private URI buildUri(String baseUrl, String path, Map<String, Object> meta) {
        String normalizedBase = trimTrailingSlash(baseUrl);
        String normalizedPath = path == null || path.isBlank() ? "" : (path.startsWith("/") ? path : "/" + path);
        if (normalizedBase.endsWith("/v1") && normalizedPath.startsWith("/v1/")) {
            normalizedPath = normalizedPath.substring(3);
        }
        if (normalizedBase.endsWith("/v4") && normalizedPath.startsWith("/v4/")) {
            normalizedPath = normalizedPath.substring(3);
        }
        String withQuery = appendQueryFromMeta(normalizedPath, meta);
        return URI.create(normalizedBase + withQuery);
    }

    private String appendQueryFromMeta(String path, Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return path;
        }
        String raw = stringVal(meta.get(ConnectionMetadataHelper.CUSTOM_QUERY_PARAMS_JSON));
        if (raw.isBlank()) {
            return path;
        }
        try {
            Map<String, Object> qp = objectMapper.readValue(raw, MAP_TYPE);
            if (qp.isEmpty()) {
                return path;
            }
            StringBuilder sb = new StringBuilder(path);
            String sep = path.contains("?") ? "&" : "?";
            for (Map.Entry<String, Object> e : qp.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) {
                    continue;
                }
                sb.append(sep)
                        .append(URLEncoder.encode(e.getKey().trim(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(e.getValue() == null ? "" : String.valueOf(e.getValue()), StandardCharsets.UTF_8));
                sep = "&";
            }
            return sb.toString();
        } catch (Exception ex) {
            return path;
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String extractMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "模型服务返回异常";
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(responseBody, MAP_TYPE);
            Object error = payload.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                Object message = errorMap.get("message");
                if (message != null) {
                    return String.valueOf(message);
                }
            }
            Object message = payload.get("message");
            if (message != null) {
                return String.valueOf(message);
            }
        } catch (Exception ignored) {
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
    }
}
