package com.example.aigc.service;

import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.model.RouterApiKey;
import com.example.aigc.model.RouterRequestLog;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.repository.RouterRequestLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class RouterProxyService {

    private final RouterApiKeyService routerApiKeyService;
    private final RouterRoutingService routerRoutingService;
    private final ModelConfigRepository modelConfigRepository;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final ProviderCatalog providerCatalog;
    private final ProviderHttpGateway providerHttpGateway;
    private final FormatConversionService formatConversionService;
    private final RouterRequestLogRepository routerRequestLogRepository;
    private final ModelCapabilityService modelCapabilityService;
    private final ObjectMapper objectMapper;

    public RouterProxyService(
            RouterApiKeyService routerApiKeyService,
            RouterRoutingService routerRoutingService,
            ModelConfigRepository modelConfigRepository,
            ApiKeyCryptoService apiKeyCryptoService,
            ProviderCatalog providerCatalog,
            ProviderHttpGateway providerHttpGateway,
            FormatConversionService formatConversionService,
            RouterRequestLogRepository routerRequestLogRepository,
            ModelCapabilityService modelCapabilityService,
            ObjectMapper objectMapper
    ) {
        this.routerApiKeyService = routerApiKeyService;
        this.routerRoutingService = routerRoutingService;
        this.modelConfigRepository = modelConfigRepository;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.providerCatalog = providerCatalog;
        this.providerHttpGateway = providerHttpGateway;
        this.formatConversionService = formatConversionService;
        this.routerRequestLogRepository = routerRequestLogRepository;
        this.modelCapabilityService = modelCapabilityService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> proxyChat(Map<String, Object> requestBody, String clientFormat, String authorization, String xApiKey) {
        RouterApiKey routerApiKey = routerApiKeyService.requireActive(authorization, xApiKey);
        return proxyChatInternal(requestBody, clientFormat, routerApiKey);
    }

    public StreamingResponseBody proxyChatStream(Map<String, Object> requestBody, String clientFormat, String authorization, String xApiKey) {
        RouterApiKey routerApiKey = routerApiKeyService.requireActive(authorization, xApiKey);
        List<ResolvedConnection> candidates = resolveCandidates(requestBody == null ? null : stringValue(requestBody.get("model")), true);
        if (candidates.isEmpty()) {
            throw new ProviderGatewayException(400, "未找到可用的文本模型连接，请先在模型配置中启用文本模型");
        }
        ResolvedConnection primary = candidates.get(0);
        Duration timeout = Duration.ofSeconds(routerRoutingService.timeoutSeconds());
        Map<String, Object> convertedRequest = new LinkedHashMap<>(formatConversionService.convertRequest(requestBody, clientFormat, primary.provider().apiFormat()));
        convertedRequest.put("stream", true);

        if (!clientFormat.equalsIgnoreCase(primary.provider().apiFormat())) {
            Map<String, Object> response = proxyChatInternal(requestBody, clientFormat, routerApiKey);
            return outputStream -> {
                outputStream.write(("data: " + objectMapper.writeValueAsString(response) + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            };
        }

        return outputStream -> {
            long start = System.currentTimeMillis();
            ProviderGatewayException lastStreamError = null;
            for (ResolvedConnection candidate : candidates) {
                for (String apiKey : candidate.apiKeys()) {
                    try (InputStream stream = providerHttpGateway.streamChat(
                            candidate.provider(),
                            candidate.connection().getBaseUrl(),
                            apiKey,
                            candidate.metadataPlain(),
                            convertedRequest,
                            timeout
                    )) {
                        stream.transferTo(outputStream);
                        outputStream.flush();
                        log(routerApiKey, candidate, stringValue(requestBody.get("model")), clientFormat, "success",
                                (int) (System.currentTimeMillis() - start), Map.of(), null);
                        return;
                    } catch (ProviderGatewayException ex) {
                        lastStreamError = ex;
                        if (ex.getStatusCode() == 401) {
                            continue;
                        }
                        break;
                    } catch (Exception ex) {
                        lastStreamError = new ProviderGatewayException(502, ex.getMessage() == null ? "流式调用失败" : ex.getMessage());
                        break;
                    }
                }
            }
            try {
                Map<String, Object> fallback = proxyChatInternal(requestBody, clientFormat, routerApiKey);
                outputStream.write(("data: " + objectMapper.writeValueAsString(fallback) + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception ex) {
                throw lastStreamError != null ? lastStreamError : new ProviderGatewayException(502, "流式调用失败");
            }
        };
    }

    public Map<String, Object> listModels(String authorization, String xApiKey) {
        routerApiKeyService.requireActive(authorization, xApiKey);
        List<ResolvedConnection> candidates = resolveCandidates(null, true);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> data = new ArrayList<>();
        for (ResolvedConnection candidate : candidates) {
            List<ModelConfig> configuredModels = candidate.textModels();
            if (!configuredModels.isEmpty()) {
                for (ModelConfig modelConfig : configuredModels) {
                    if (seen.add(modelConfig.getModelName())) {
                        data.add(Map.of(
                                "id", modelConfig.getModelName(),
                                "object", "model",
                                "owned_by", candidate.provider().key()
                        ));
                    }
                }
                continue;
            }
            List<String> remoteModels = providerHttpGateway.listModels(
                    candidate.provider(),
                    candidate.connection().getBaseUrl(),
                    candidate.apiKeys().isEmpty() ? "" : candidate.apiKeys().get(0),
                    candidate.metadataPlain(),
                    Duration.ofSeconds(8)
            );
            for (String model : remoteModels) {
                if (seen.add(model)) {
                    data.add(Map.of(
                            "id", model,
                            "object", "model",
                            "owned_by", candidate.provider().key()
                    ));
                }
            }
        }
        return Map.of("object", "list", "data", data);
    }

    private Map<String, Object> proxyChatInternal(Map<String, Object> requestBody, String clientFormat, RouterApiKey routerApiKey) {
        long start = System.currentTimeMillis();
        String requestedModel = requestBody == null ? "" : stringValue(requestBody.get("model"));
        List<ResolvedConnection> candidates = resolveCandidates(requestedModel, true);
        ProviderGatewayException lastError = null;

        boolean firstConn = true;
        for (ResolvedConnection candidate : candidates) {
            Map<String, Object> convertedRequest = new LinkedHashMap<>(formatConversionService.convertRequest(requestBody, clientFormat, candidate.provider().apiFormat()));
            convertedRequest.put("stream", false);
            boolean firstKey = true;
            for (String apiKey : candidate.apiKeys()) {
                try {
                    Map<String, Object> response = providerHttpGateway.invokeChat(
                            candidate.provider(),
                            candidate.connection().getBaseUrl(),
                            apiKey,
                            candidate.metadataPlain(),
                            convertedRequest,
                            Duration.ofSeconds(routerRoutingService.timeoutSeconds())
                    );
                    Map<String, Integer> usage = formatConversionService.extractUsage(response, candidate.provider().apiFormat());
                    Map<String, Object> convertedResponse = formatConversionService.convertResponse(
                            response,
                            candidate.provider().apiFormat(),
                            clientFormat,
                            requestedModel
                    );
                    String status = firstConn && firstKey ? "success" : "failover";
                    log(routerApiKey, candidate, requestedModel, clientFormat, status, (int) (System.currentTimeMillis() - start), usage, null);
                    return convertedResponse;
                } catch (ProviderGatewayException ex) {
                    lastError = ex;
                    if (ex.getStatusCode() == 401) {
                        firstKey = false;
                        continue;
                    }
                    break;
                }
            }
            firstConn = false;
        }

        log(routerApiKey, null, requestedModel, clientFormat, "error",
                (int) (System.currentTimeMillis() - start), Map.of(), lastError == null ? "所有模型服务均调用失败" : lastError.getMessage());
        if (lastError != null) {
            throw lastError;
        }
        throw new ProviderGatewayException(500, "未找到可用的模型服务");
    }

    private List<ResolvedConnection> resolveCandidates(String requestedModel, boolean includeFailoverCandidates) {
        List<ConnectionConfig> orderedConnections = routerRoutingService.resolveOrderedConnections(includeFailoverCandidates);
        List<ModelConfig> enabledTextModels = modelConfigRepository.findAll().stream()
                .filter(ModelConfig::isEnabled)
                .filter(model -> modelCapabilityService.supports(model, "text"))
                .toList();

        List<ResolvedConnection> result = new ArrayList<>();
        for (ConnectionConfig connection : orderedConnections) {
            ProviderCatalog.ProviderDefinition provider = providerCatalog.require(connection.getProvider());
            if (!provider.textProxySupported()) {
                continue;
            }
            List<ModelConfig> connectionModels = enabledTextModels.stream()
                    .filter(model -> connection.getId().equals(model.getConnectionId()))
                    .toList();
            if (requestedModel != null && !requestedModel.isBlank() && !connectionModels.isEmpty()) {
                boolean matched = connectionModels.stream().anyMatch(model -> requestedModel.equalsIgnoreCase(model.getModelName()));
                if (!matched) {
                    continue;
                }
            }
            Map<String, Object> metaPlain = ConnectionMetadataHelper.decryptForUse(connection.getMetadata(), apiKeyCryptoService);
            List<String> apiKeys = resolveRouterApiKeys(provider, connection, metaPlain);
            result.add(new ResolvedConnection(connection, provider, apiKeys, metaPlain, connectionModels));
        }
        return result;
    }

    private List<String> resolveRouterApiKeys(ProviderCatalog.ProviderDefinition provider, ConnectionConfig connection, Map<String, Object> metaPlain) {
        List<String> keys = new ArrayList<>();
        if (provider.gatewayKind() == GatewayKind.BEDROCK) {
            if (connection.getEncryptedApiKey() != null && !connection.getEncryptedApiKey().isBlank()) {
                keys.add(apiKeyCryptoService.decrypt(connection.getEncryptedApiKey()));
            }
            return keys.isEmpty() ? List.of("") : keys;
        }
        if (provider.gatewayKind() == GatewayKind.VERTEX) {
            return List.of("");
        }
        if (provider.authMode() == ProviderCatalog.AuthMode.NONE) {
            return List.of("");
        }
        if (connection.getEncryptedApiKey() != null && !connection.getEncryptedApiKey().isBlank()) {
            String primary = apiKeyCryptoService.decrypt(connection.getEncryptedApiKey());
            if (!primary.isBlank()) {
                keys.add(primary);
            }
        }
        String extraBlock = stringValue(metaPlain.get(ConnectionMetadataHelper.EXTRA_API_KEYS));
        if (!extraBlock.isBlank()) {
            LinkedHashSet<String> seen = new LinkedHashSet<>(keys);
            for (String line : extraBlock.split("\r?\n")) {
                String t = line.trim();
                if (!t.isEmpty() && seen.add(t)) {
                    keys.add(t);
                }
            }
        }
        return keys.isEmpty() ? List.of("") : keys;
    }

    private void log(
            RouterApiKey apiKey,
            ResolvedConnection connection,
            String model,
            String requestFormat,
            String status,
            int durationMs,
            Map<String, Integer> usage,
            String errorMessage
    ) {
        RouterRequestLog log = RouterRequestLog.create();
        log.setRouterApiKeyId(apiKey == null ? null : apiKey.getId());
        if (connection != null) {
            log.setConnectionId(connection.connection().getId());
            log.setConnectionName(connection.connection().getName());
            log.setProvider(connection.provider().key());
        }
        log.setModel(model);
        log.setRequestFormat(requestFormat);
        log.setStatus(status);
        log.setDurationMs(durationMs);
        log.setPromptTokens(usage.getOrDefault("prompt_tokens", 0));
        log.setCompletionTokens(usage.getOrDefault("completion_tokens", 0));
        log.setTotalTokens(usage.getOrDefault("total_tokens", 0));
        log.setErrorMessage(errorMessage);
        routerRequestLogRepository.save(log);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ResolvedConnection(
            ConnectionConfig connection,
            ProviderCatalog.ProviderDefinition provider,
            List<String> apiKeys,
            Map<String, Object> metadataPlain,
            List<ModelConfig> textModels
    ) {
    }
}
