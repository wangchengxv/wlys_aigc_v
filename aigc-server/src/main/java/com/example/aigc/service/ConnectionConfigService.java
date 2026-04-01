package com.example.aigc.service;

import com.example.aigc.dto.ConnectionConfigCreateRequest;
import com.example.aigc.dto.ConnectionConfigResponse;
import com.example.aigc.dto.ConnectionConfigUpdateRequest;
import com.example.aigc.dto.QuickConnectionRequest;
import com.example.aigc.dto.RouterConnectionTestResponse;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.model.PresetModel;
import com.example.aigc.model.PresetModelRegistry;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.service.ProviderCatalog.AuthMode;
import com.example.aigc.service.ProviderCatalog.ProviderDefinition;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConnectionConfigService {

    private final ConnectionConfigRepository connectionConfigRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final PresetModelRegistry presetModelRegistry;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final ProviderCatalog providerCatalog;
    private final ModelCapabilityService modelCapabilityService;
    private final RouterRoutingService routerRoutingService;
    private final ProviderHttpGateway providerHttpGateway;

    public ConnectionConfigService(
            ConnectionConfigRepository connectionConfigRepository,
            ModelConfigRepository modelConfigRepository,
            PresetModelRegistry presetModelRegistry,
            ApiKeyCryptoService apiKeyCryptoService,
            ProviderCatalog providerCatalog,
            ModelCapabilityService modelCapabilityService,
            RouterRoutingService routerRoutingService,
            ProviderHttpGateway providerHttpGateway
    ) {
        this.connectionConfigRepository = connectionConfigRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.presetModelRegistry = presetModelRegistry;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.providerCatalog = providerCatalog;
        this.modelCapabilityService = modelCapabilityService;
        this.routerRoutingService = routerRoutingService;
        this.providerHttpGateway = providerHttpGateway;
    }

    public ConnectionConfigResponse create(ConnectionConfigCreateRequest request) {
        ProviderDefinition definition = providerCatalog.require(request.provider());
        String rawKey = request.apiKey() == null ? "" : request.apiKey().trim();
        validateApiKeyForProvider(definition, rawKey, request.metadata());
        Map<String, Object> metaStored = ConnectionMetadataHelper.normalizeIncoming(request.metadata(), apiKeyCryptoService);

        String toEncrypt = "";
        if (definition.gatewayKind() == GatewayKind.BEDROCK) {
            toEncrypt = rawKey;
        } else if (definition.authMode() != AuthMode.NONE) {
            toEncrypt = rawKey;
        }
        String encryptedApiKey = apiKeyCryptoService.encrypt(toEncrypt);
        ConnectionConfig config = ConnectionConfig.create(
                request.name(),
                definition.key(),
                normalizeBaseUrl(request.baseUrl(), definition),
                encryptedApiKey,
                request.enabled(),
                metaStored
        );
        connectionConfigRepository.save(config);
        routerRoutingService.appendConnectionIfAbsent(config.getId());
        return toResponse(config);
    }

    private void validateApiKeyForProvider(ProviderDefinition definition, String rawKey, Map<String, Object> metadata) {
        if (definition.gatewayKind() == GatewayKind.BEDROCK) {
            if (rawKey.isEmpty()) {
                throw new BizException(400, ErrorCode.BAD_REQUEST, "请填写 AWS Secret Access Key");
            }
            Map<String, Object> m = metadata == null ? Map.of() : metadata;
            if (stringVal(m.get(ConnectionMetadataHelper.AWS_ACCESS_KEY_ID)).isBlank()) {
                throw new BizException(400, ErrorCode.BAD_REQUEST, "请填写 AWS Access Key ID（metadata.awsAccessKeyId）");
            }
            if (stringVal(m.get(ConnectionMetadataHelper.AWS_REGION)).isBlank()) {
                throw new BizException(400, ErrorCode.BAD_REQUEST, "请填写 AWS Region（metadata.region）");
            }
            return;
        }
        if (definition.gatewayKind() == GatewayKind.VERTEX) {
            Map<String, Object> m = metadata == null ? Map.of() : metadata;
            if (stringVal(m.get(ConnectionMetadataHelper.VERTEX_PROJECT)).isBlank()
                    || stringVal(m.get(ConnectionMetadataHelper.VERTEX_LOCATION)).isBlank()) {
                throw new BizException(400, ErrorCode.BAD_REQUEST, "请填写 Vertex 项目 ID 与区域（metadata）");
            }
            if (stringVal(m.get(ConnectionMetadataHelper.VERTEX_SA_JSON)).isBlank()) {
                throw new BizException(400, ErrorCode.BAD_REQUEST, "请填写 Service Account JSON");
            }
            return;
        }
        if (definition.authMode() != AuthMode.NONE && rawKey.isEmpty()) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "API Key 不能为空");
        }
    }

    private static String stringVal(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    public List<ConnectionConfigResponse> list() {
        return connectionConfigRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ConnectionConfigResponse get(String id) {
        ConnectionConfig config = connectionConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "连接配置不存在"));
        return toResponse(config);
    }

    public ConnectionConfigResponse update(String id, ConnectionConfigUpdateRequest request) {
        ConnectionConfig config = connectionConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "连接配置不存在"));
        String provider = request.provider() == null || request.provider().isBlank() ? config.getProvider() : providerCatalog.require(request.provider()).key();
        ProviderDefinition definition = providerCatalog.require(provider);
        if (request.name() != null && !request.name().isBlank()) {
            config.setName(request.name());
        }
        config.setProvider(provider);
        if (request.baseUrl() != null && !request.baseUrl().isBlank()) {
            config.setBaseUrl(normalizeBaseUrl(request.baseUrl(), definition));
        } else if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            config.setBaseUrl(definition.defaultBaseUrl());
        }
        if (request.enabled() != null) {
            config.setEnabled(request.enabled());
        }
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setEncryptedApiKey(apiKeyCryptoService.encrypt(request.apiKey().trim()));
        }
        if (request.metadata() != null) {
            config.setMetadata(ConnectionMetadataHelper.merge(config.getMetadata(), request.metadata(), apiKeyCryptoService));
        }
        config.touch();
        connectionConfigRepository.save(config);
        return toResponse(config);
    }

    public void delete(String id) {
        if (!connectionConfigRepository.existsById(id)) {
            throw new BizException(404, ErrorCode.NOT_FOUND, "连接配置不存在");
        }
        modelConfigRepository.findAll().stream()
                .filter(model -> id.equals(model.getConnectionId()))
                .forEach(model -> modelConfigRepository.deleteById(model.getId()));
        connectionConfigRepository.deleteById(id);
        routerRoutingService.removeConnection(id);
    }

    public ConnectionConfigResponse quickCreate(QuickConnectionRequest request) {
        PresetModel preset = presetModelRegistry.find(request.provider(), request.modelName());
        if (preset == null) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "该模型不在预置库中，请使用高级模式配置");
        }

        String encryptedApiKey = apiKeyCryptoService.encrypt(request.apiKey());
        String connectionName = preset.getProvider() + " - " + preset.getDisplayName();
        ConnectionConfig connection = ConnectionConfig.create(
                connectionName,
                preset.getProvider(),
                normalizeBaseUrl(preset.getBaseUrl(), providerCatalog.require(preset.getProvider())),
                encryptedApiKey,
                request.enabled() != null ? request.enabled() : true,
                new HashMap<>()
        );
        connectionConfigRepository.save(connection);
        routerRoutingService.appendConnectionIfAbsent(connection.getId());

        ModelConfig model = ModelConfig.create(
                preset.getDisplayName(),
                preset.getProvider(),
                preset.getModelName(),
                connection.getId(),
                true,
                modelCapabilityService.mergeCapabilities(new HashMap<>(), preset.getCapabilities())
        );
        modelConfigRepository.save(model);

        return toResponse(connection);
    }

    public RouterConnectionTestResponse test(String id) {
        ConnectionConfig config = connectionConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "连接配置不存在"));
        ProviderDefinition definition = providerCatalog.require(config.getProvider());
        Map<String, Object> metaPlain = ConnectionMetadataHelper.decryptForUse(config.getMetadata(), apiKeyCryptoService);
        String apiKey = resolvePlainApiKey(definition, config, metaPlain);
        try {
            List<String> models = providerHttpGateway.listModels(definition, config.getBaseUrl(), apiKey, metaPlain, Duration.ofSeconds(8));
            return new RouterConnectionTestResponse(true, "连接测试通过", models);
        } catch (ProviderGatewayException ex) {
            return new RouterConnectionTestResponse(false, ex.getMessage(), List.of());
        }
    }

    private String resolvePlainApiKey(ProviderDefinition definition, ConnectionConfig config, Map<String, Object> metaPlain) {
        if (definition.gatewayKind() == GatewayKind.BEDROCK) {
            if (config.getEncryptedApiKey() == null || config.getEncryptedApiKey().isBlank()) {
                return "";
            }
            return apiKeyCryptoService.decrypt(config.getEncryptedApiKey());
        }
        if (definition.gatewayKind() == GatewayKind.VERTEX) {
            return "";
        }
        if (definition.authMode() == AuthMode.NONE) {
            return "";
        }
        if (config.getEncryptedApiKey() == null || config.getEncryptedApiKey().isBlank()) {
            return "";
        }
        return apiKeyCryptoService.decrypt(config.getEncryptedApiKey());
    }

    private ConnectionConfigResponse toResponse(ConnectionConfig config) {
        String plainApiKey = "";
        if (config.getEncryptedApiKey() != null && !config.getEncryptedApiKey().isBlank()) {
            plainApiKey = apiKeyCryptoService.decrypt(config.getEncryptedApiKey());
        }
        return new ConnectionConfigResponse(
                config.getId(),
                config.getName(),
                config.getProvider(),
                config.getBaseUrl(),
                apiKeyCryptoService.mask(plainApiKey),
                !plainApiKey.isBlank(),
                config.isEnabled(),
                ConnectionMetadataHelper.maskForResponse(config.getMetadata()),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private String normalizeBaseUrl(String baseUrl, ProviderDefinition definition) {
        String normalized = (baseUrl == null || baseUrl.isBlank()) ? definition.defaultBaseUrl() : baseUrl.trim();
        URI uri;
        try {
            uri = new URI(normalized);
        } catch (URISyntaxException ex) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "Base URL 格式不正确");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"https".equals(scheme)) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "Base URL 仅支持 https 协议");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "Base URL 缺少主机名");
        }
        if (isPrivateOrLocalHost(host)) {
            throw new BizException(400, ErrorCode.BAD_REQUEST, "Base URL 不允许使用本地或内网地址");
        }
        return uri.toString();
    }

    private boolean isPrivateOrLocalHost(String host) {
        String h = host.trim().toLowerCase();
        if ("localhost".equals(h) || h.endsWith(".localhost")) {
            return true;
        }
        if (!isIpv4Literal(h)) {
            return false;
        }
        return h.startsWith("10.")
                || h.startsWith("127.")
                || h.startsWith("169.254.")
                || h.startsWith("192.168.")
                || is172PrivateRange(h);
    }

    private boolean isIpv4Literal(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isBlank()) {
                return false;
            }
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }

    private boolean is172PrivateRange(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return host.startsWith("172.") && second >= 16 && second <= 31;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
