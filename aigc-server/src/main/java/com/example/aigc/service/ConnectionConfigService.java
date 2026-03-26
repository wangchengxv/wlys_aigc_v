package com.example.aigc.service;

import com.example.aigc.dto.ConnectionConfigCreateRequest;
import com.example.aigc.dto.ConnectionConfigResponse;
import com.example.aigc.dto.ConnectionConfigUpdateRequest;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionConfigService {

    private final ConnectionConfigRepository connectionConfigRepository;
    private final ApiKeyCryptoService apiKeyCryptoService;

    public ConnectionConfigService(ConnectionConfigRepository connectionConfigRepository, ApiKeyCryptoService apiKeyCryptoService) {
        this.connectionConfigRepository = connectionConfigRepository;
        this.apiKeyCryptoService = apiKeyCryptoService;
    }

    public ConnectionConfigResponse create(ConnectionConfigCreateRequest request) {
        String encryptedApiKey = apiKeyCryptoService.encrypt(request.apiKey());
        ConnectionConfig config = ConnectionConfig.create(
                request.name(),
                request.provider(),
                request.baseUrl(),
                encryptedApiKey,
                request.enabled()
        );
        connectionConfigRepository.save(config);
        return toResponse(config);
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
        config.setName(request.name());
        config.setProvider(request.provider());
        config.setBaseUrl(request.baseUrl());
        config.setEnabled(request.enabled());
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setEncryptedApiKey(apiKeyCryptoService.encrypt(request.apiKey()));
        }
        config.touch();
        connectionConfigRepository.save(config);
        return toResponse(config);
    }

    public void delete(String id) {
        if (!connectionConfigRepository.existsById(id)) {
            throw new BizException(404, ErrorCode.NOT_FOUND, "连接配置不存在");
        }
        connectionConfigRepository.deleteById(id);
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
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}