package com.example.aigc.service;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.model.PresetModelRegistry;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiCapabilityRoutingService {

    private final ModelConfigRepository modelConfigRepository;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final ProviderCatalog providerCatalog;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final ModelCapabilityService modelCapabilityService;
    private final RouterRoutingService routerRoutingService;
    private final AigcArkProperties arkProperties;
    private final PresetModelRegistry presetModelRegistry;

    public AiCapabilityRoutingService(
            ModelConfigRepository modelConfigRepository,
            ConnectionConfigRepository connectionConfigRepository,
            ProviderCatalog providerCatalog,
            ApiKeyCryptoService apiKeyCryptoService,
            ModelCapabilityService modelCapabilityService,
            RouterRoutingService routerRoutingService,
            AigcArkProperties arkProperties,
            PresetModelRegistry presetModelRegistry
    ) {
        this.modelConfigRepository = modelConfigRepository;
        this.connectionConfigRepository = connectionConfigRepository;
        this.providerCatalog = providerCatalog;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.modelCapabilityService = modelCapabilityService;
        this.routerRoutingService = routerRoutingService;
        this.arkProperties = arkProperties;
        this.presetModelRegistry = presetModelRegistry;
    }

    public ResolvedAiModel resolveText(String explicitModelName) {
        return resolve("text", explicitModelName);
    }

    public ResolvedAiModel resolveImage(String explicitModelName) {
        return resolve("image", explicitModelName);
    }

    public ResolvedAiModel resolveVideo(String explicitModelName) {
        return resolve("video", explicitModelName);
    }

    public ResolvedAiModel resolveTts(String explicitModelName) {
        return resolve("tts", explicitModelName);
    }

    public ResolvedAiModel resolve(String capability, String explicitModelName) {
        List<ModelConfig> candidates = modelConfigRepository.findAll().stream()
                .filter(ModelConfig::isEnabled)
                .filter(model -> modelCapabilityService.supports(model, capability))
                .toList();

        if (explicitModelName != null && !explicitModelName.isBlank()) {
            for (ModelConfig candidate : candidates) {
                String matchedBy = matchExplicitModel(candidate, explicitModelName.trim());
                if (matchedBy != null) {
                    ResolvedAiModel resolved = toResolved(candidate, capability, true);
                    if (resolved != null) {
                        return resolved.withMatch(matchedBy);
                    }
                }
            }
            ResolvedAiModel presetFallback = tryResolveViaPreset(capability, explicitModelName.trim());
            if (presetFallback != null) {
                return presetFallback;
            }
            return fallback(capability, explicitModelName.trim(), "NO_MATCH");
        }

        List<String> orderedConnectionIds = routerRoutingService.resolveOrderedConnections(true).stream()
                .map(ConnectionConfig::getId)
                .toList();

        for (String connectionId : orderedConnectionIds) {
            for (ModelConfig candidate : candidates) {
                if (connectionId.equals(candidate.getConnectionId())) {
                    ResolvedAiModel resolved = toResolved(candidate, capability, false);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }

        for (ModelConfig candidate : candidates) {
            ResolvedAiModel resolved = toResolved(candidate, capability, false);
            if (resolved != null) {
                return resolved;
            }
        }

        return fallback(capability, null, "NO_ENABLED_MODEL");
    }

    private ResolvedAiModel tryResolveViaPreset(String capability, String modelName) {
        for (var p : presetModelRegistry.getAll()) {
            if (!p.getCapabilities().contains(capability)) {
                continue;
            }
            if (!normalize(p.getModelName()).equals(normalize(modelName))) {
                continue;
            }
            var connOpt = connectionConfigRepository.findAll().stream()
                    .filter(c -> c.isEnabled() && providerCatalog.require(c.getProvider()).key().equalsIgnoreCase(p.getProvider()))
                    .findFirst();
            if (connOpt.isEmpty()) {
                continue;
            }
            ConnectionConfig conn = connOpt.get();
            ProviderCatalog.ProviderDefinition providerDef = providerCatalog.require(p.getProvider());
            if (!supportsInvocation(providerDef, capability)) {
                continue;
            }
            Map<String, Object> metaPlain = ConnectionMetadataHelper.decryptForUse(conn.getMetadata(), apiKeyCryptoService);
            String apiKey = resolvePlainApiKeyWithoutExplicitCheck(conn, providerDef);
            if (apiKey == null) {
                continue;
            }
            return new ResolvedAiModel(
                    capability,
                    modelName,
                    "PRESET_FALLBACK",
                    "preset-model",
                    null,
                    conn,
                    providerDef,
                    apiKey,
                    metaPlain,
                    true
            );
        }
        return null;
    }

    private String resolvePlainApiKeyWithoutExplicitCheck(ConnectionConfig conn, ProviderCatalog.ProviderDefinition provider) {
        if (provider.gatewayKind() == GatewayKind.BEDROCK) {
            String encrypted = conn.getEncryptedApiKey();
            if (encrypted == null || encrypted.isBlank()) return null;
            return apiKeyCryptoService.decrypt(encrypted);
        }
        if (provider.gatewayKind() == GatewayKind.VERTEX) return "";
        if (provider.authMode() != ProviderCatalog.AuthMode.NONE) {
            String encrypted = conn.getEncryptedApiKey();
            if (encrypted == null || encrypted.isBlank()) return null;
            return apiKeyCryptoService.decrypt(encrypted);
        }
        return "";
    }

    private ResolvedAiModel toResolved(ModelConfig modelConfig, String capability, boolean explicitSelection) {
        ConnectionConfig connectionConfig = connectionConfigRepository.findById(modelConfig.getConnectionId()).orElse(null);
        if (connectionConfig == null || !connectionConfig.isEnabled()) {
            return null;
        }
        ProviderCatalog.ProviderDefinition provider = providerCatalog.require(connectionConfig.getProvider());
        if (!supportsInvocation(provider, capability)) {
            if (explicitSelection && "video".equals(capability)) {
                throw new BizException(400, "当前视频模型对应连接未配置视频接口");
            }
            if (explicitSelection && "image".equals(capability)) {
                throw new BizException(400, "当前图片模型对应连接未配置图片接口");
            }
            return null;
        }
        Map<String, Object> metaPlain = ConnectionMetadataHelper.decryptForUse(connectionConfig.getMetadata(), apiKeyCryptoService);
        String apiKey = resolvePlainApiKey(connectionConfig, provider, explicitSelection);
        if (apiKey == null) {
            return null;
        }
        return new ResolvedAiModel(
                capability,
                modelConfig.getModelName(),
                "USER_CONFIGURED",
                "modelName",
                null,
                connectionConfig,
                provider,
                apiKey,
                metaPlain,
                false
        );
    }

    private String resolvePlainApiKey(ConnectionConfig connectionConfig, ProviderCatalog.ProviderDefinition provider, boolean explicitSelection) {
        if (provider.gatewayKind() == GatewayKind.BEDROCK) {
            String encryptedApiKey = connectionConfig.getEncryptedApiKey();
            if (encryptedApiKey == null || encryptedApiKey.isBlank()) {
                if (explicitSelection) {
                    throw new BizException(400, "连接未配置 Secret Access Key：" + connectionConfig.getName());
                }
                return null;
            }
            return apiKeyCryptoService.decrypt(encryptedApiKey);
        }
        if (provider.gatewayKind() == GatewayKind.VERTEX) {
            return "";
        }
        if (provider.authMode() != ProviderCatalog.AuthMode.NONE) {
            String encryptedApiKey = connectionConfig.getEncryptedApiKey();
            if (encryptedApiKey == null || encryptedApiKey.isBlank()) {
                if (explicitSelection) {
                    throw new BizException(400, "连接未配置 API Key：" + connectionConfig.getName());
                }
                return null;
            }
            return apiKeyCryptoService.decrypt(encryptedApiKey);
        }
        return "";
    }

    private boolean supportsInvocation(ProviderCatalog.ProviderDefinition provider, String capability) {
        return switch (capability) {
            case "text" -> provider.textProxySupported() && textGatewayReady(provider);
            case "image" -> provider.imageGenerationPath() != null && !provider.imageGenerationPath().isBlank();
            case "video" -> provider.videoSubmitPath() != null && !provider.videoSubmitPath().isBlank()
                    && provider.videoResultPath() != null && !provider.videoResultPath().isBlank();
            case "tts" -> provider.textProxySupported() && textGatewayReady(provider);
            default -> false;
        };
    }

    private boolean textGatewayReady(ProviderCatalog.ProviderDefinition provider) {
        if (provider.gatewayKind() == GatewayKind.BEDROCK || provider.gatewayKind() == GatewayKind.VERTEX) {
            return true;
        }
        return provider.chatPath() != null && !provider.chatPath().isBlank();
    }

    private ResolvedAiModel fallback(String capability, String explicitModelName, String rejectReason) {
        if ("image".equals(capability)) {
            return new ResolvedAiModel(
                    capability,
                    explicitModelName == null || explicitModelName.isBlank() ? arkProperties.getDefaultImageModel() : explicitModelName,
                    "SYSTEM_FALLBACK",
                    explicitModelName == null || explicitModelName.isBlank() ? "default-image" : "explicit-no-match",
                    rejectReason,
                    null,
                    providerCatalog.require("ark"),
                    arkProperties.getApiKey() == null ? "" : arkProperties.getApiKey(),
                    new HashMap<>(),
                    true
            );
        }
        if ("video".equals(capability)) {
            return new ResolvedAiModel(
                    capability,
                    explicitModelName == null || explicitModelName.isBlank() ? arkProperties.getDefaultVideoModel() : explicitModelName,
                    "SYSTEM_FALLBACK",
                    explicitModelName == null || explicitModelName.isBlank() ? "default-video" : "explicit-no-match",
                    rejectReason,
                    null,
                    providerCatalog.require("ark"),
                    arkProperties.getApiKey() == null ? "" : arkProperties.getApiKey(),
                    new HashMap<>(),
                    true
            );
        }
        return new ResolvedAiModel(
                capability,
                explicitModelName == null || explicitModelName.isBlank() ? "mock-text" : explicitModelName,
                "SYSTEM_FALLBACK",
                explicitModelName == null || explicitModelName.isBlank() ? "default-text" : "explicit-no-match",
                rejectReason,
                null,
                null,
                "",
                new HashMap<>(),
                true
        );
    }

    private String matchExplicitModel(ModelConfig modelConfig, String explicitModelName) {
        String expected = normalize(explicitModelName);
        if (expected.isBlank()) {
            return null;
        }
        if (expected.equals(normalize(modelConfig.getModelName()))) {
            return "modelName";
        }
        if (expected.equals(normalize(modelConfig.getName()))) {
            return "name";
        }
        for (String alias : resolveAliases(modelConfig.getMetadata())) {
            if (expected.equals(normalize(alias))) {
                return "alias";
            }
        }
        return null;
    }

    private List<String> resolveAliases(Map<String, Object> metadata) {
        List<String> aliases = new ArrayList<>();
        if (metadata == null) {
            return aliases;
        }
        Object raw = metadata.get("aliases");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String value = String.valueOf(item).trim();
                    if (!value.isBlank()) {
                        aliases.add(value);
                    }
                }
            }
            return aliases;
        }
        if (raw instanceof String text) {
            String[] split = text.split(",");
            for (String item : split) {
                String value = item.trim();
                if (!value.isBlank()) {
                    aliases.add(value);
                }
            }
        }
        return aliases;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedAiModel(
            String capability,
            String modelName,
            String source,
            String matchedBy,
            String rejectReason,
            ConnectionConfig connection,
            ProviderCatalog.ProviderDefinition provider,
            String apiKey,
            Map<String, Object> metadataPlain,
            boolean systemFallback
    ) {
        public boolean hasProvider() {
            return provider != null;
        }

        public ResolvedAiModel withMatch(String match) {
            return new ResolvedAiModel(capability, modelName, source, match, rejectReason, connection, provider, apiKey, metadataPlain, systemFallback);
        }
    }
}
