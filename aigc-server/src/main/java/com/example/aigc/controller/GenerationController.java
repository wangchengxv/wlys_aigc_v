package com.example.aigc.controller;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.ImageModelOptionsData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.dto.VideoModelOptionsData;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.exception.BizException;
import com.example.aigc.service.GenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class GenerationController {
    private final GenerationService generationService;
    private final AigcArkProperties arkProperties;

    public GenerationController(GenerationService generationService, AigcArkProperties arkProperties) {
        this.generationService = generationService;
        this.arkProperties = arkProperties;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateResponseData> generate(@Valid @RequestBody GenerateRequest request) {
        return ApiResponse.ok(generationService.generate(request));
    }

    @GetMapping("/history")
    public ApiResponse<PagedResult<GenerateResponseData>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String mode
    ) {
        GenerateMode generateMode = parseMode(mode);
        return ApiResponse.ok(generationService.history(page, pageSize, generateMode));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<GenerateResponseData> detail(@PathVariable String taskId) {
        return ApiResponse.ok(generationService.taskDetail(taskId));
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of("ok", true, "service", "aigc-server", "version", "v1"));
    }

    @GetMapping("/models/image")
    public ApiResponse<ImageModelOptionsData> imageModels() {
        ArrayList<String> options = new ArrayList<>(
                arkProperties.getImageModelOptions() == null ? List.of() : arkProperties.getImageModelOptions()
        );
        if (!options.contains(arkProperties.getDefaultImageModel())) {
            options.add(0, arkProperties.getDefaultImageModel());
        }
        return ApiResponse.ok(new ImageModelOptionsData(
                arkProperties.getDefaultImageModel(),
                options
        ));
    }

    @GetMapping("/models/video")
    public ApiResponse<VideoModelOptionsData> videoModels() {
        ArrayList<String> options = new ArrayList<>(
                arkProperties.getVideoModelOptions() == null ? List.of() : arkProperties.getVideoModelOptions()
        );
        if (!options.contains(arkProperties.getDefaultVideoModel())) {
            options.add(0, arkProperties.getDefaultVideoModel());
        }
        return ApiResponse.ok(new VideoModelOptionsData(
                arkProperties.getDefaultVideoModel(),
                options
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
}
