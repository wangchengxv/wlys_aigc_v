package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.dto.RouterApiKeyCreateRequest;
import com.example.aigc.dto.RouterApiKeyResponse;
import com.example.aigc.dto.RouterRequestLogResponse;
import com.example.aigc.dto.RouterRoutingResponse;
import com.example.aigc.dto.RouterRoutingUpdateRequest;
import com.example.aigc.dto.RouterStatsResponse;
import com.example.aigc.service.RouterAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/router")
public class RouterAdminController {

    private final RouterAdminService routerAdminService;

    public RouterAdminController(RouterAdminService routerAdminService) {
        this.routerAdminService = routerAdminService;
    }

    @GetMapping("/keys")
    public ApiResponse<java.util.List<RouterApiKeyResponse>> keys() {
        return ApiResponse.ok(routerAdminService.listApiKeys());
    }

    @PostMapping("/keys")
    public ApiResponse<RouterApiKeyResponse> createKey(@RequestBody @Valid RouterApiKeyCreateRequest request) {
        return ApiResponse.ok(routerAdminService.createApiKey(request.name()));
    }

    @PatchMapping("/keys/{id}")
    public ApiResponse<RouterApiKeyResponse> toggleKey(@PathVariable String id, @RequestBody Map<String, Object> body) {
        boolean active = Boolean.parseBoolean(String.valueOf(body.getOrDefault("active", true)));
        return ApiResponse.ok(routerAdminService.toggleApiKey(id, active));
    }

    @DeleteMapping("/keys/{id}")
    public ApiResponse<Void> deleteKey(@PathVariable String id) {
        routerAdminService.deleteApiKey(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/routing")
    public ApiResponse<RouterRoutingResponse> routing() {
        return ApiResponse.ok(routerAdminService.getRouting());
    }

    @PutMapping("/routing")
    public ApiResponse<RouterRoutingResponse> updateRouting(@RequestBody RouterRoutingUpdateRequest request) {
        return ApiResponse.ok(routerAdminService.updateRouting(request));
    }

    @GetMapping("/logs")
    public ApiResponse<PagedResult<RouterRequestLogResponse>> logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String routerApiKeyId,
            @RequestParam(required = false) String connectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer days
    ) {
        return ApiResponse.ok(routerAdminService.listLogs(page, pageSize, routerApiKeyId, connectionId, status, days));
    }

    @GetMapping("/stats")
    public ApiResponse<RouterStatsResponse> stats() {
        return ApiResponse.ok(routerAdminService.stats());
    }

    @GetMapping("/config/export")
    public ApiResponse<Map<String, Object>> exportConfig() {
        return ApiResponse.ok(routerAdminService.exportConfig());
    }

    @PostMapping("/config/import")
    public ApiResponse<Void> importConfig(@RequestBody Map<String, Object> body) {
        routerAdminService.importConfig(body);
        return ApiResponse.ok(null);
    }
}
