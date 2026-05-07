package com.example.aigc.service;

import com.example.aigc.dto.PagedResult;
import com.example.aigc.dto.RouterApiKeyResponse;
import com.example.aigc.dto.RouterRequestLogResponse;
import com.example.aigc.dto.RouterRoutingResponse;
import com.example.aigc.dto.RouterRoutingUpdateRequest;
import com.example.aigc.dto.RouterStatsResponse;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.model.RouterApiKey;
import com.example.aigc.model.RouterRequestLog;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.repository.RouterApiKeyRepository;
import com.example.aigc.repository.RouterRequestLogRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouterAdminService {

    private final RouterApiKeyService routerApiKeyService;
    private final RouterRoutingService routerRoutingService;
    private final RouterRequestLogRepository routerRequestLogRepository;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final RouterApiKeyRepository routerApiKeyRepository;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final ProviderCatalog providerCatalog;

    public RouterAdminService(
            RouterApiKeyService routerApiKeyService,
            RouterRoutingService routerRoutingService,
            RouterRequestLogRepository routerRequestLogRepository,
            ConnectionConfigRepository connectionConfigRepository,
            ModelConfigRepository modelConfigRepository,
            RouterApiKeyRepository routerApiKeyRepository,
            ApiKeyCryptoService apiKeyCryptoService,
            ProviderCatalog providerCatalog
    ) {
        this.routerApiKeyService = routerApiKeyService;
        this.routerRoutingService = routerRoutingService;
        this.routerRequestLogRepository = routerRequestLogRepository;
        this.connectionConfigRepository = connectionConfigRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.routerApiKeyRepository = routerApiKeyRepository;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.providerCatalog = providerCatalog;
    }

    public RouterRoutingResponse getRouting() {
        return routerRoutingService.getResponse();
    }

    public RouterRoutingResponse updateRouting(RouterRoutingUpdateRequest request) {
        return routerRoutingService.update(request);
    }

    public PagedResult<RouterRequestLogResponse> listLogs(int page, int pageSize, String routerApiKeyId, String connectionId, String status, Integer days) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        Instant since = days == null ? null : Instant.now().minus(days, ChronoUnit.DAYS);

        List<RouterRequestLog> filtered = routerRequestLogRepository.findAll().stream()
                .filter(log -> routerApiKeyId == null || routerApiKeyId.isBlank() || routerApiKeyId.equals(log.getRouterApiKeyId()))
                .filter(log -> connectionId == null || connectionId.isBlank() || connectionId.equals(log.getConnectionId()))
                .filter(log -> status == null || status.isBlank() || status.equalsIgnoreCase(log.getStatus()))
                .filter(log -> since == null || (log.getTimestamp() != null && !log.getTimestamp().isBefore(since)))
                .toList();

        int from = Math.max(0, (safePage - 1) * safePageSize);
        int to = Math.min(filtered.size(), from + safePageSize);
        List<RouterRequestLogResponse> list = from >= filtered.size()
                ? List.of()
                : filtered.subList(from, to).stream().map(this::toResponse).toList();
        return new PagedResult<>(list, filtered.size());
    }

    public RouterStatsResponse stats() {
        List<RouterRequestLog> logs = routerRequestLogRepository.findAll();
        Instant now = Instant.now();
        Instant dayAgo = now.minus(1, ChronoUnit.DAYS);
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant monthAgo = now.minus(30, ChronoUnit.DAYS);
        return new RouterStatsResponse(
                countSince(logs, dayAgo),
                countSince(logs, weekAgo),
                countSince(logs, monthAgo),
                tokensSince(logs, dayAgo),
                tokensSince(logs, weekAgo),
                tokensSince(logs, monthAgo),
                logs.size(),
                logs.stream().mapToLong(RouterRequestLog::getTotalTokens).sum()
        );
    }

    public Map<String, Object> exportConfig() {
        List<Map<String, Object>> connections = connectionConfigRepository.findAll().stream()
                .map(connection -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", connection.getId());
                    item.put("name", connection.getName());
                    item.put("provider", connection.getProvider());
                    item.put("baseUrl", connection.getBaseUrl());
                    item.put("apiKey", decryptIfPresent(connection.getEncryptedApiKey()));
                    item.put("enabled", connection.isEnabled());
                    item.put("createdAt", connection.getCreatedAt());
                    item.put("updatedAt", connection.getUpdatedAt());
                    return item;
                })
                .toList();

        List<Map<String, Object>> models = modelConfigRepository.findAll().stream()
                .map(model -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", model.getId());
                    item.put("name", model.getName());
                    item.put("provider", model.getProvider());
                    item.put("modelName", model.getModelName());
                    item.put("connectionId", model.getConnectionId());
                    item.put("enabled", model.isEnabled());
                    item.put("metadata", model.getMetadata());
                    item.put("createdAt", model.getCreatedAt());
                    item.put("updatedAt", model.getUpdatedAt());
                    return item;
                })
                .toList();

        List<Map<String, Object>> routerApiKeys = routerApiKeyRepository.findAll().stream()
                .map(key -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", key.getId());
                    item.put("name", key.getName());
                    item.put("keyValue", key.getKeyValue());
                    item.put("active", key.isActive());
                    item.put("createdAt", key.getCreatedAt());
                    item.put("lastUsedAt", key.getLastUsedAt());
                    return item;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("connections", connections);
        payload.put("models", models);
        payload.put("routerApiKeys", routerApiKeys);
        payload.put("routing", routerRoutingService.getConfig());
        return payload;
    }

    @SuppressWarnings("unchecked")
    public void importConfig(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "导入内容不能为空");
        }

        Object connectionsNode = payload.get("connections");
        if (connectionsNode instanceof List<?> connections) {
            for (Object item : connections) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) raw;
                ConnectionConfig connection = new ConnectionConfig();
                connection.setId(stringValue(map.get("id")));
                connection.setName(stringValue(map.get("name")));
                connection.setProvider(providerCatalog.require(stringValue(map.get("provider"))).key());
                connection.setBaseUrl(stringValue(map.get("baseUrl")));
                String apiKey = stringValue(map.get("apiKey"));
                if (!apiKey.isBlank()) {
                    connection.setEncryptedApiKey(apiKeyCryptoService.encrypt(apiKey));
                }
                connection.setEnabled(Boolean.parseBoolean(String.valueOf(map.getOrDefault("enabled", true))));
                connection.setCreatedAt(parseInstant(map.get("createdAt")));
                connection.setUpdatedAt(parseInstant(map.get("updatedAt")));
                if (connection.getId() != null && !connection.getId().isBlank()) {
                    connectionConfigRepository.save(connection);
                }
            }
        }

        Object modelsNode = payload.get("models");
        if (modelsNode instanceof List<?> models) {
            for (Object item : models) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) raw;
                ModelConfig model = new ModelConfig();
                model.setId(stringValue(map.get("id")));
                model.setName(stringValue(map.get("name")));
                model.setProvider(providerCatalog.require(stringValue(map.get("provider"))).key());
                model.setModelName(stringValue(map.get("modelName")));
                model.setConnectionId(stringValue(map.get("connectionId")));
                model.setEnabled(Boolean.parseBoolean(String.valueOf(map.getOrDefault("enabled", true))));
                Object metadata = map.get("metadata");
                model.setMetadata(metadata instanceof Map<?, ?> meta ? new LinkedHashMap<>((Map<String, Object>) meta) : new LinkedHashMap<>());
                model.setCreatedAt(parseInstant(map.get("createdAt")));
                model.setUpdatedAt(parseInstant(map.get("updatedAt")));
                if (model.getId() != null && !model.getId().isBlank()) {
                    modelConfigRepository.save(model);
                }
            }
        }

        Object keysNode = payload.get("routerApiKeys");
        if (keysNode instanceof List<?> keys) {
            for (Object item : keys) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) raw;
                RouterApiKey apiKey = new RouterApiKey();
                apiKey.setId(stringValue(map.get("id")));
                apiKey.setName(stringValue(map.get("name")));
                apiKey.setKeyValue(stringValue(map.get("keyValue")));
                apiKey.setActive(Boolean.parseBoolean(String.valueOf(map.getOrDefault("active", true))));
                apiKey.setCreatedAt(parseInstant(map.get("createdAt")));
                apiKey.setLastUsedAt(parseInstant(map.get("lastUsedAt")));
                if (apiKey.getId() != null && !apiKey.getId().isBlank() && apiKey.getKeyValue() != null && !apiKey.getKeyValue().isBlank()) {
                    routerApiKeyRepository.save(apiKey);
                }
            }
        }

        Object routingNode = payload.get("routing");
        if (routingNode instanceof Map<?, ?> routingRaw) {
            Map<String, Object> routing = (Map<String, Object>) routingRaw;
            List<com.example.aigc.dto.TimeScheduleSlotDto> slots = new ArrayList<>();
            Object timeScheduleNode = routing.get("timeSchedule");
            if (timeScheduleNode instanceof List<?> scheduleList) {
                for (Object item : scheduleList) {
                    if (item instanceof Map<?, ?> scheduleMap) {
                        slots.add(new com.example.aigc.dto.TimeScheduleSlotDto(
                                stringValue(scheduleMap.get("start")),
                                stringValue(scheduleMap.get("end")),
                                stringValue(scheduleMap.get("connectionId"))
                        ));
                    }
                }
            }
            routerRoutingService.update(new com.example.aigc.dto.RouterRoutingUpdateRequest(
                    stringValue(routing.get("strategy")),
                    asStringList(routing.get("priorityConnectionIds")),
                    routing.get("failoverEnabled") == null ? null : Boolean.parseBoolean(String.valueOf(routing.get("failoverEnabled"))),
                    routing.get("failoverTimeoutSeconds") == null ? null : Integer.parseInt(String.valueOf(routing.get("failoverTimeoutSeconds"))),
                    slots
            ));
        }
    }

    public List<RouterApiKeyResponse> listApiKeys() {
        return routerApiKeyService.list();
    }

    public RouterApiKeyResponse createApiKey(String name) {
        return routerApiKeyService.create(name);
    }

    public void deleteApiKey(String id) {
        routerApiKeyService.delete(id);
    }

    public RouterApiKeyResponse toggleApiKey(String id, boolean active) {
        return routerApiKeyService.toggle(id, active);
    }

    private RouterRequestLogResponse toResponse(RouterRequestLog log) {
        return new RouterRequestLogResponse(
                log.getId(),
                log.getTimestamp(),
                log.getRouterApiKeyId(),
                log.getConnectionId(),
                log.getConnectionName(),
                log.getProvider(),
                log.getModel(),
                log.getRequestFormat(),
                log.getStatus(),
                log.getDurationMs(),
                log.getPromptTokens(),
                log.getCompletionTokens(),
                log.getTotalTokens(),
                log.getErrorMessage()
        );
    }

    private long countSince(List<RouterRequestLog> logs, Instant since) {
        return logs.stream().filter(log -> log.getTimestamp() != null && !log.getTimestamp().isBefore(since)).count();
    }

    private long tokensSince(List<RouterRequestLog> logs, Instant since) {
        return logs.stream()
                .filter(log -> log.getTimestamp() != null && !log.getTimestamp().isBefore(since))
                .mapToLong(RouterRequestLog::getTotalTokens)
                .sum();
    }

    private String decryptIfPresent(String encryptedApiKey) {
        if (encryptedApiKey == null || encryptedApiKey.isBlank()) {
            return "";
        }
        return apiKeyCryptoService.decrypt(encryptedApiKey);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        try {
            return Instant.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
