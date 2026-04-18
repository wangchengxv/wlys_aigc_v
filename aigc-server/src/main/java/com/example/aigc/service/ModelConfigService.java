package com.example.aigc.service;

import com.example.aigc.dto.BatchModelsImportRequest;
import com.example.aigc.dto.ModelConfigCreateRequest;
import com.example.aigc.dto.ModelConfigResponse;
import com.example.aigc.dto.ModelConfigUpdateRequest;
import com.example.aigc.dto.ModelProbeResponse;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.service.ProviderCatalog.AuthMode;
import com.example.aigc.service.ProviderCatalog.ProviderDefinition;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelConfigService {

    private final ModelConfigRepository modelConfigRepository;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final ProviderCatalog providerCatalog;
    private final ModelCapabilityService modelCapabilityService;
    private final ProviderHttpGateway providerHttpGateway;
    private final ApiKeyCryptoService apiKeyCryptoService;

    public ModelConfigService(
            ModelConfigRepository modelConfigRepository,
            ConnectionConfigRepository connectionConfigRepository,
            ProviderCatalog providerCatalog,
            ModelCapabilityService modelCapabilityService,
            ProviderHttpGateway providerHttpGateway,
            ApiKeyCryptoService apiKeyCryptoService
    ) {
        this.modelConfigRepository = modelConfigRepository;
        this.connectionConfigRepository = connectionConfigRepository;
        this.providerCatalog = providerCatalog;
        this.modelCapabilityService = modelCapabilityService;
        this.providerHttpGateway = providerHttpGateway;
        this.apiKeyCryptoService = apiKeyCryptoService;
    }

    public ModelConfigResponse create(ModelConfigCreateRequest request) {
        validateConnection(request.connectionId());
        ModelConfig modelConfig = ModelConfig.create(
                request.name(),
                providerCatalog.require(request.provider()).key(),
                request.modelName(),
                request.connectionId(),
                request.enabled(),
                normalizeMetadata(request.metadata(), request.provider(), request.modelName())
        );
        modelConfigRepository.save(modelConfig);
        return toResponse(modelConfig);
    }

    public List<ModelConfigResponse> list() {
        return modelConfigRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ModelConfigResponse get(String id) {
        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "模型配置不存在"));
        return toResponse(modelConfig);
    }

    public ModelConfigResponse update(String id, ModelConfigUpdateRequest request) {
        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "模型配置不存在"));
        String connectionId = request.connectionId() == null || request.connectionId().isBlank() ? modelConfig.getConnectionId() : request.connectionId();
        validateConnection(connectionId);
        String provider = request.provider() == null || request.provider().isBlank() ? modelConfig.getProvider() : providerCatalog.require(request.provider()).key();
        String modelName = request.modelName() == null || request.modelName().isBlank() ? modelConfig.getModelName() : request.modelName();

        if (request.name() != null && !request.name().isBlank()) {
            modelConfig.setName(request.name());
        }
        modelConfig.setProvider(provider);
        modelConfig.setModelName(modelName);
        modelConfig.setConnectionId(connectionId);
        if (request.enabled() != null) {
            modelConfig.setEnabled(request.enabled());
        }
        modelConfig.setMetadata(normalizeMetadata(
                request.metadata() == null ? modelConfig.getMetadata() : request.metadata(),
                provider,
                modelName
        ));
        modelConfig.touch();
        modelConfigRepository.save(modelConfig);
        return toResponse(modelConfig);
    }

    public void delete(String id) {
        modelConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "模型配置不存在"));
        modelConfigRepository.deleteById(id);
    }

    public ModelProbeResponse probe(String modelId) {
        ModelConfig model = modelConfigRepository.findById(modelId)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "模型配置不存在"));
        ConnectionConfig conn = connectionConfigRepository.findById(model.getConnectionId())
                .orElseThrow(() -> new BizException(400, ErrorCode.BAD_REQUEST, "关联连接不存在"));
        ProviderDefinition def = providerCatalog.require(conn.getProvider());
        Map<String, Object> metaPlain = ConnectionMetadataHelper.decryptForUse(conn.getMetadata(), apiKeyCryptoService);
        String apiKey = resolveProbeApiKey(def, conn);
        Map<String, Object> payload = buildProbePayload(def, model.getModelName());
        try {
            providerHttpGateway.invokeChat(def, conn.getBaseUrl(), apiKey, metaPlain, payload, Duration.ofSeconds(20));
            return new ModelProbeResponse(true, "探测成功");
        } catch (ProviderGatewayException ex) {
            return new ModelProbeResponse(false, ex.getMessage());
        }
    }

    private String resolveProbeApiKey(ProviderDefinition def, ConnectionConfig conn) {
        if (def.gatewayKind() == GatewayKind.BEDROCK) {
            if (conn.getEncryptedApiKey() == null || conn.getEncryptedApiKey().isBlank()) {
                return "";
            }
            return apiKeyCryptoService.decrypt(conn.getEncryptedApiKey());
        }
        if (def.gatewayKind() == GatewayKind.VERTEX) {
            return "";
        }
        if (def.authMode() == AuthMode.NONE) {
            return "";
        }
        if (conn.getEncryptedApiKey() == null || conn.getEncryptedApiKey().isBlank()) {
            return "";
        }
        return apiKeyCryptoService.decrypt(conn.getEncryptedApiKey());
    }

    private Map<String, Object> buildProbePayload(ProviderDefinition def, String modelName) {
        if ("anthropic".equalsIgnoreCase(def.apiFormat())) {
            return Map.of(
                    "model", modelName,
                    "max_tokens", 16,
                    "messages", List.of(Map.of("role", "user", "content", "ping"))
            );
        }
        return Map.of(
                "model", modelName,
                "max_tokens", 16,
                "messages", List.of(Map.of("role", "user", "content", "ping"))
        );
    }

    public List<ModelConfigResponse> batchImport(BatchModelsImportRequest request) {
        validateConnection(request.connectionId());
        ConnectionConfig conn = connectionConfigRepository.findById(request.connectionId())
                .orElseThrow(() -> new BizException(400, ErrorCode.BAD_REQUEST, "关联连接不存在"));
        ProviderDefinition def = providerCatalog.require(conn.getProvider());
        List<String> caps = request.capabilities();
        if (caps == null || caps.isEmpty()) {
            caps = List.of("text");
        }
        List<ModelConfigResponse> out = new ArrayList<>();
        for (String raw : request.modelNames()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String modelName = raw.trim();
            String displayName = modelName;
            java.util.Map<String, Object> meta = new HashMap<>();
            meta.put("capabilities", new ArrayList<>(caps));
            ModelConfig modelConfig = ModelConfig.create(
                    displayName,
                    def.key(),
                    modelName,
                    request.connectionId(),
                    true,
                    normalizeMetadata(meta, def.key(), modelName)
            );
            modelConfigRepository.save(modelConfig);
            out.add(toResponse(modelConfig));
        }
        return out;
    }

    private void validateConnection(String connectionId) {
        if (!connectionConfigRepository.existsById(connectionId)) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "关联连接配置不存在");
        }
    }

    private ModelConfigResponse toResponse(ModelConfig modelConfig) {
        return new ModelConfigResponse(
                modelConfig.getId(),
                modelConfig.getName(),
                modelConfig.getProvider(),
                modelConfig.getModelName(),
                modelConfig.getConnectionId(),
                modelConfig.isEnabled(),
                modelConfig.getMetadata(),
                modelConfig.getCreatedAt(),
                modelConfig.getUpdatedAt()
        );
    }

    public List<ModelConfig> listEnabledByCapability(String capability) {
        return modelConfigRepository.findAll().stream()
                .filter(ModelConfig::isEnabled)
                .filter(model -> modelCapabilityService.supports(model, capability))
                .toList();
    }

    private java.util.Map<String, Object> normalizeMetadata(java.util.Map<String, Object> metadata, String provider, String modelName) {
        java.util.Map<String, Object> source = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        return modelCapabilityService.normalizeModelMetadata(source, provider, modelName);
    }
}
