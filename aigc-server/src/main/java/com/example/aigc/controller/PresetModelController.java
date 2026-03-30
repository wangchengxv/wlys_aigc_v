package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.PresetModelListResponse;
import com.example.aigc.model.PresetModelRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preset-models")
public class PresetModelController {

    private final PresetModelRegistry registry;

    public PresetModelController(PresetModelRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ApiResponse<PresetModelListResponse> list() {
        var models = registry.getAll().stream()
                .map(PresetModelListResponse.PresetModelDto::from)
                .toList();
        var providers = registry.getProviders();
        return ApiResponse.ok(new PresetModelListResponse(models, providers));
    }
}
