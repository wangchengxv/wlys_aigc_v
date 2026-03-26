package com.example.aigc.service.impl;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.entity.GenerationTask;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.enums.TaskStatus;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.GenerationTaskRepository;
import com.example.aigc.service.GenerationService;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GenerationServiceImpl implements GenerationService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<String> BLOCK_WORDS = List.of("暴恐", "色情", "违禁", "涉政");

    private final GenerationTaskRepository repository;
    private final AigcArkProperties arkProperties;
    private final RestClient restClient;

    public GenerationServiceImpl(GenerationTaskRepository repository, AigcArkProperties arkProperties) {
        this.repository = repository;
        this.arkProperties = arkProperties;
        this.restClient = RestClient.builder().baseUrl(arkProperties.getBaseUrl()).build();
    }

    @Override
    public GenerateResponseData generate(GenerateRequest request) {
        validatePrompt(request.prompt());

        long start = System.currentTimeMillis();
        String taskId = "T" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
        GenerationTask task = new GenerationTask();
        task.setTaskId(taskId);
        task.setPrompt(request.prompt());
        task.setMode(request.mode());
        task.setStyle(request.style());
        task.setCreatedAt(LocalDateTime.now());
        task.setStatus(TaskStatus.SUCCESS);

        List<String> textResults = new ArrayList<>();
        List<String> imageResults = new ArrayList<>();
        List<String> videoResults = new ArrayList<>();

        if (request.mode() == GenerateMode.text || request.mode() == GenerateMode.both) {
            textResults = mockText(request.prompt(), request.style(), request.textLength());
        }
        if (request.mode() == GenerateMode.image || request.mode() == GenerateMode.both) {
            String selectedModel = resolveImageModel(request.imageModel());
            imageResults = generateImagesFromArk(request.prompt(), safeCount(request.count()), selectedModel);
            task.setImageModel(selectedModel);
        }
        if (request.mode() == GenerateMode.video) {
            String selectedModel = resolveVideoModel(request.videoModel());
            videoResults = generateVideosFromArk(request.prompt(), safeCount(request.count()), selectedModel);
            task.setVideoModel(selectedModel);
        }

        task.setTextResults(textResults);
        task.setImageResults(imageResults);
        task.setVideoResults(videoResults);
        task.setLatencyMs(System.currentTimeMillis() - start);
        repository.save(task);
        return toData(task);
    }

    @Override
    public PagedResult<GenerateResponseData> history(int page, int pageSize, GenerateMode mode) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(Math.min(pageSize, 50), 1);
        List<GenerateResponseData> list = repository.page(safePage, safePageSize, mode).stream().map(this::toData).toList();
        return new PagedResult<>(list, repository.count(mode));
    }

    @Override
    public GenerateResponseData taskDetail(String taskId) {
        GenerationTask task = repository.findByTaskId(taskId).orElseThrow(() -> new BizException(404, "任务不存在"));
        return toData(task);
    }

    private void validatePrompt(String prompt) {
        for (String word : BLOCK_WORDS) {
            if (prompt.contains(word)) {
                throw new BizException(400, "输入内容包含敏感词，请调整后重试");
            }
        }
    }

    private int safeCount(Integer count) {
        if (count == null) {
            return 1;
        }
        return Math.max(1, Math.min(count, 4));
    }

    private String resolveImageModel(String imageModel) {
        if (imageModel == null || imageModel.isBlank()) {
            return arkProperties.getDefaultImageModel();
        }
        return imageModel.trim();
    }

    private String resolveVideoModel(String videoModel) {
        if (videoModel == null || videoModel.isBlank()) {
            return arkProperties.getDefaultVideoModel();
        }
        return videoModel.trim();
    }

    private List<String> mockText(String prompt, String style, String textLength) {
        String len = textLength == null ? "medium" : textLength;
        String desc = switch (len) {
            case "short" -> "短文案";
            case "long" -> "长文案";
            default -> "中等长度文案";
        };
        return List.of(
                "【" + style + desc + "】围绕\"" + prompt + "\"，建议突出卖点与场景体验，结尾加入行动指令。",
                "主题：" + prompt + "。结构建议采用“痛点-亮点-福利-行动”，强化传播和转化。"
        );
    }

    private List<String> generateImagesFromArk(String prompt, int count, String imageModel) {
        if (arkProperties.getApiKey() == null || arkProperties.getApiKey().isBlank()) {
            throw new BizException(500, "服务未配置ARK_API_KEY，请联系管理员");
        }
        List<String> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            JsonResponse data = callArkImageApi(prompt, imageModel);
            String imageUrl = data.imageUrl();
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new BizException(502, "模型服务返回异常，未获取到图片地址");
            }
            images.add(imageUrl);
        }
        return images;
    }

    private JsonResponse callArkImageApi(String prompt, String imageModel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", imageModel);
        payload.put("prompt", prompt);
        payload.put("sequential_image_generation", arkProperties.getSequentialImageGeneration());
        payload.put("response_format", arkProperties.getResponseFormat());
        payload.put("size", arkProperties.getSize());
        payload.put("stream", arkProperties.isStream());
        payload.put("watermark", arkProperties.isWatermark());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.post()
                    .uri("/api/v3/images/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + arkProperties.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return parseArkResponse(body);
        } catch (HttpStatusCodeException ex) {
            int code = ex.getStatusCode().value();
            if (code == 401 || code == 403) {
                throw new BizException(502, "模型服务鉴权失败，请检查ARK_API_KEY");
            }
            throw new BizException(502, "模型服务调用失败，状态码：" + code);
        } catch (ResourceAccessException ex) {
            throw new BizException(504, "模型服务请求超时，请稍后重试");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(502, "模型服务调用失败，请稍后重试");
        }
    }

    private List<String> generateVideosFromArk(String prompt, int count, String videoModel) {
        if (arkProperties.getApiKey() == null || arkProperties.getApiKey().isBlank()) {
            throw new BizException(500, "服务未配置ARK_API_KEY，请联系管理员");
        }
        List<String> videos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String videoUrl = callArkVideoApi(prompt, videoModel);
            if (videoUrl == null || videoUrl.isBlank()) {
                throw new BizException(502, "模型服务返回异常，未获取到视频地址");
            }
            videos.add(videoUrl);
        }
        return videos;
    }

    private String callArkVideoApi(String prompt, String videoModel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", videoModel);
        // 与方舟「内容生成 / 视频」接口一致：content 数组 + 文本内嵌参数（见 Doubao-Seedance 官方示例）
        int duration = safeVideoDuration();
        boolean watermark = arkProperties.isWatermark();
        String textWithParams = prompt.trim()
                + " --duration " + duration
                + " --camerafixed false --watermark " + watermark;
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", textWithParams);
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(textContent);
        payload.put("content", content);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> submitBody = restClient.post()
                    .uri(arkProperties.getVideoApiPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + arkProperties.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String directUrl = parseArkVideoUrl(submitBody, false);
            if (directUrl != null) {
                return directUrl;
            }

            String taskId = parseArkVideoTaskId(submitBody);
            if (taskId == null) {
                throw new BizException(502, "视频模型服务返回异常，缺少task_id或视频地址");
            }
            return pollArkVideoTask(taskId);
        } catch (HttpStatusCodeException ex) {
            int code = ex.getStatusCode().value();
            if (code == 401 || code == 403) {
                throw new BizException(502, "模型服务鉴权失败，请检查ARK_API_KEY");
            }
            throw new BizException(502, "视频模型服务调用失败，状态码：" + code);
        } catch (ResourceAccessException ex) {
            throw new BizException(504, "视频模型服务请求超时，请稍后重试");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(502, "视频模型服务调用失败，请稍后重试");
        }
    }

    private int safeVideoDuration() {
        Integer raw = arkProperties.getVideoDurationSeconds();
        if (raw == null) {
            return 5;
        }
        return Math.max(1, Math.min(raw, 30));
    }

    private String pollArkVideoTask(String taskId) {
        int maxAttempts = Math.max(1, arkProperties.getVideoPollMaxAttempts());
        long intervalMs = Math.max(300L, arkProperties.getVideoPollIntervalMs());
        String lastStatus = null;
        Map<String, Object> lastBody = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> resultBody = requestVideoTaskResult(taskId);
            lastBody = resultBody;
            String maybeUrl = parseArkVideoUrl(resultBody, false);
            if (maybeUrl != null) {
                return maybeUrl;
            }

            String status = parseArkTaskStatus(resultBody);
            lastStatus = status;
            if (isSuccessStatus(status)) {
                if (attempt == maxAttempts) {
                    throw new BizException(502, "视频任务已完成，但未返回视频地址，task_id="
                            + taskId + "，status=" + safeStatus(status) + "，响应摘要=" + summarizeBody(resultBody));
                }
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

    private Map<String, Object> requestVideoTaskResult(String taskId) {
        String resultPath = arkProperties.getVideoResultApiPath();
        try {
            if (resultPath != null && resultPath.contains("{taskId}")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = restClient.get()
                        .uri(resultPath, taskId)
                        .header("Authorization", "Bearer " + arkProperties.getApiKey())
                        .retrieve()
                        .body(Map.class);
                return body;
            }
            Map<String, Object> payload = Map.of("task_id", taskId);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.post()
                    .uri(resultPath == null || resultPath.isBlank() ? arkProperties.getVideoApiPath() : resultPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + arkProperties.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return body;
        } catch (HttpStatusCodeException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404) {
                throw new BizException(502, "视频任务查询接口不存在，请检查video-result-api-path配置");
            }
            throw new BizException(502, "视频任务查询失败，状态码：" + code);
        } catch (ResourceAccessException ex) {
            throw new BizException(504, "视频任务查询超时，请稍后重试");
        }
    }

    private JsonResponse parseArkResponse(Map<String, Object> body) {
        if (body == null) {
            throw new BizException(502, "模型服务返回为空");
        }
        Object dataNode = body.get("data");
        if (!(dataNode instanceof List<?> dataList) || dataList.isEmpty()) {
            throw new BizException(502, "模型服务返回异常，缺少data字段");
        }
        Object first = dataList.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            throw new BizException(502, "模型服务返回异常，图片结果格式错误");
        }
        Object url = firstMap.get("url");
        return new JsonResponse(url == null ? null : String.valueOf(url));
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
                nestedValue(body, "data", "status"),
                nestedValue(body, "result", "status"),
                nestedValue(body, "output", "status")
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

    private record JsonResponse(String imageUrl) {
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
                task.getVideoModel()
        );
    }
}
