package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ModelConfigCreateRequest;
import com.example.aigc.dto.ModelConfigResponse;
import com.example.aigc.dto.ModelConfigUpdateRequest;
import com.example.aigc.service.ModelConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @PostMapping
    public ApiResponse<ModelConfigResponse> create(@RequestBody @Valid ModelConfigCreateRequest request) {
        return ApiResponse.ok(modelConfigService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ModelConfigResponse>> list() {
        return ApiResponse.ok(modelConfigService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelConfigResponse> get(@PathVariable String id) {
        return ApiResponse.ok(modelConfigService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelConfigResponse> update(
            @PathVariable String id,
            @RequestBody ModelConfigUpdateRequest request
    ) {
        return ApiResponse.ok(modelConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        modelConfigService.delete(id);
        return ApiResponse.ok(null);
    }
}