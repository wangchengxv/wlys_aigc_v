package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.CanvasGraphDto;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.dto.SaveCanvasRequest;
import com.example.aigc.service.CanvasGraphService;
import com.example.aigc.service.RequestAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/canvas")
public class CanvasGraphController {
    private final CanvasGraphService canvasGraphService;
    private final RequestAuthService requestAuthService;

    public CanvasGraphController(CanvasGraphService canvasGraphService, RequestAuthService requestAuthService) {
        this.canvasGraphService = canvasGraphService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping
    public ApiResponse<PagedResult<CanvasGraphDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(canvasGraphService.list(ownerId, projectId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<CanvasGraphDto> get(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(canvasGraphService.get(ownerId, id));
    }

    @PostMapping
    public ApiResponse<CanvasGraphDto> save(
            @Valid @RequestBody SaveCanvasRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(canvasGraphService.createOrOverwrite(ownerId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CanvasGraphDto> update(
            @PathVariable String id,
            @Valid @RequestBody SaveCanvasRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(canvasGraphService.update(ownerId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        canvasGraphService.delete(ownerId, id);
        return ApiResponse.ok(null);
    }
}
