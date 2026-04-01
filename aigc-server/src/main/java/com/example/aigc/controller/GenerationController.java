package com.example.aigc.controller;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.ImageModelOptionsData;
import com.example.aigc.dto.ModelOptionDetailData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.dto.VideoModelOptionsData;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.service.GenerationService;
import com.example.aigc.service.ModelConfigService;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RouterRoutingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class GenerationController {
    private final GenerationService generationService;
    private final AigcArkProperties arkProperties;
    private final ModelConfigService modelConfigService;
    private final RouterRoutingService routerRoutingService;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final RequestAuthService requestAuthService;

    public GenerationController(
            GenerationService generationService,
            AigcArkProperties arkProperties,
            ModelConfigService modelConfigService,
            RouterRoutingService routerRoutingService,
            ConnectionConfigRepository connectionConfigRepository,
            RequestAuthService requestAuthService
    ) {
        this.generationService = generationService;
        this.arkProperties = arkProperties;
        this.modelConfigService = modelConfigService;
        this.routerRoutingService = routerRoutingService;
        this.connectionConfigRepository = connectionConfigRepository;
        this.requestAuthService = requestAuthService;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateResponseData> generate(
            @Valid @RequestBody GenerateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(generationService.generate(request, ownerId));
    }

    @GetMapping("/history")
    public ApiResponse<PagedResult<GenerateResponseData>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String mode,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        GenerateMode generateMode = parseMode(mode);
        return ApiResponse.ok(generationService.history(page, pageSize, generateMode, ownerId));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<GenerateResponseData> detail(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(generationService.taskDetail(taskId, ownerId));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> delete(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        generationService.deleteTask(taskId, ownerId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of("ok", true, "service", "aigc-server", "version", "v1"));
    }

    @GetMapping("/models/image")
    public ApiResponse<ImageModelOptionsData> imageModels() {
        List<ModelConfig> configured = modelConfigService.listEnabledByCapability("image");
        if (!configured.isEmpty()) {
            List<String> options = configured.stream().map(ModelConfig::getModelName).distinct().toList();
            return ApiResponse.ok(new ImageModelOptionsData(
                    resolveDefaultModelName(configured, options),
                    options,
                    buildModelDetails(configured, "image")
            ));
        }
        ArrayList<String> options = new ArrayList<>(arkProperties.getImageModelOptions() == null ? List.of() : arkProperties.getImageModelOptions());
        if (!options.contains(arkProperties.getDefaultImageModel())) {
            options.add(0, arkProperties.getDefaultImageModel());
        }
        return ApiResponse.ok(new ImageModelOptionsData(
                arkProperties.getDefaultImageModel(),
                options,
                List.of()
        ));
    }

    @GetMapping("/models/video")
    public ApiResponse<VideoModelOptionsData> videoModels() {
        List<ModelConfig> configured = modelConfigService.listEnabledByCapability("video");
        if (!configured.isEmpty()) {
            List<String> options = configured.stream().map(ModelConfig::getModelName).distinct().toList();
            return ApiResponse.ok(new VideoModelOptionsData(
                    resolveDefaultModelName(configured, options),
                    options,
                    buildModelDetails(configured, "video")
            ));
        }
        ArrayList<String> options = new ArrayList<>(arkProperties.getVideoModelOptions() == null ? List.of() : arkProperties.getVideoModelOptions());
        if (!options.contains(arkProperties.getDefaultVideoModel())) {
            options.add(0, arkProperties.getDefaultVideoModel());
        }
        return ApiResponse.ok(new VideoModelOptionsData(
                arkProperties.getDefaultVideoModel(),
                options,
                List.of()
        ));
    }

    private GenerateMode parseMode(String mode) {
        if (mode == null || mode.isBlank() || "all".equalsIgnoreCase(mode)) {
            return null;
        }
        try {
            return GenerateMode.valueOf(mode);
        } catch (Exception ex) {
            throw new BizException(400, "mode仅支持text/image/both/video/all");
        }
    }

    private String resolveDefaultModelName(List<ModelConfig> configured, List<String> options) {
        List<ConnectionConfig> orderedConnections = routerRoutingService.resolveOrderedConnections(true);
        for (ConnectionConfig connection : orderedConnections) {
            for (ModelConfig model : configured) {
                if (connection.getId().equals(model.getConnectionId())) {
                    return model.getModelName();
                }
            }
        }
        return options.get(0);
    }

    private List<ModelOptionDetailData> buildModelDetails(List<ModelConfig> configured, String capability) {
        return configured.stream().map(model -> {
            ConnectionConfig connection = connectionConfigRepository.findById(model.getConnectionId()).orElse(null);
            return new ModelOptionDetailData(
                    model.getModelName(),
                    model.getName(),
                    model.getProvider(),
                    capability,
                    model.isEnabled(),
                    connection != null && connection.isEnabled()
            );
        }).toList();
    }
}
