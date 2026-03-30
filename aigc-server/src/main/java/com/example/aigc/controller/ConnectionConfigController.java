package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ConnectionConfigCreateRequest;
import com.example.aigc.dto.ConnectionConfigResponse;
import com.example.aigc.dto.ConnectionConfigUpdateRequest;
import com.example.aigc.dto.QuickConnectionRequest;
import com.example.aigc.dto.RouterConnectionTestResponse;
import com.example.aigc.service.ConnectionConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionConfigController {

    private final ConnectionConfigService connectionConfigService;

    public ConnectionConfigController(ConnectionConfigService connectionConfigService) {
        this.connectionConfigService = connectionConfigService;
    }

    @PostMapping
    public ApiResponse<ConnectionConfigResponse> create(@RequestBody @Valid ConnectionConfigCreateRequest request) {
        return ApiResponse.ok(connectionConfigService.create(request));
    }

    @PostMapping("/quick")
    public ApiResponse<ConnectionConfigResponse> quickCreate(@RequestBody @Valid QuickConnectionRequest request) {
        return ApiResponse.ok(connectionConfigService.quickCreate(request));
    }

    @GetMapping
    public ApiResponse<List<ConnectionConfigResponse>> list() {
        return ApiResponse.ok(connectionConfigService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ConnectionConfigResponse> get(@PathVariable String id) {
        return ApiResponse.ok(connectionConfigService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConnectionConfigResponse> update(
            @PathVariable String id,
            @RequestBody ConnectionConfigUpdateRequest request
    ) {
        return ApiResponse.ok(connectionConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        connectionConfigService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<RouterConnectionTestResponse> test(@PathVariable String id) {
        return ApiResponse.ok(connectionConfigService.test(id));
    }
}
