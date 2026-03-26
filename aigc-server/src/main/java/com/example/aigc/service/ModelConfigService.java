package com.example.aigc.service;

import com.example.aigc.dto.ModelConfigCreateRequest;
import com.example.aigc.dto.ModelConfigResponse;
import com.example.aigc.dto.ModelConfigUpdateRequest;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class ModelConfigService {

    private final ModelConfigRepository modelConfigRepository;
    private final ConnectionConfigRepository connectionConfigRepository;

    public ModelConfigService(ModelConfigRepository modelConfigRepository, ConnectionConfigRepository connectionConfigRepository) {
        this.modelConfigRepository = modelConfigRepository;
        this.connectionConfigRepository = connectionConfigRepository;
    }

    public ModelConfigResponse create(ModelConfigCreateRequest request) {
        validateConnection(request.connectionId());
        ModelConfig modelConfig = ModelConfig.create(
                request.name(),
                request.provider(),
                request.modelName(),
                request.connectionId(),
                request.enabled(),
                request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata())
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
        validateConnection(request.connectionId());
        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "模型配置不存在"));

        modelConfig.setName(request.name());
        modelConfig.setProvider(request.provider());
        modelConfig.setModelName(request.modelName());
        modelConfig.setConnectionId(request.connectionId());
        modelConfig.setEnabled(request.enabled());
        modelConfig.setMetadata(request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata()));
        modelConfig.touch();
        modelConfigRepository.save(modelConfig);
        return toResponse(modelConfig);
    }

    public void delete(String id) {
        modelConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "模型配置不存在"));
        modelConfigRepository.deleteById(id);
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
}