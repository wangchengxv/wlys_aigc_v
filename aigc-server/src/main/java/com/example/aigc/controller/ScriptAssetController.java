package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.UpdateAssetRequest;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.enums.AssetType;
import com.example.aigc.service.ScriptWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/script-projects/{projectId}/assets")
public class ScriptAssetController {

    private final ScriptWorkflowService scriptWorkflowService;

    public ScriptAssetController(ScriptWorkflowService scriptWorkflowService) {
        this.scriptWorkflowService = scriptWorkflowService;
    }

    @PostMapping("/extract/characters")
    public ApiResponse<List<ExtractedAsset>> extractCharacters(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.extractAssets(projectId, AssetType.CHARACTER));
    }

    @PostMapping("/extract/backgrounds")
    public ApiResponse<List<ExtractedAsset>> extractBackgrounds(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.extractAssets(projectId, AssetType.BACKGROUND));
    }

    @PostMapping("/extract/props")
    public ApiResponse<List<ExtractedAsset>> extractProps(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.extractAssets(projectId, AssetType.PROP));
    }

    @GetMapping
    public ApiResponse<List<ExtractedAsset>> list(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.listAssets(projectId));
    }

    @PutMapping("/{assetId}")
    public ApiResponse<ExtractedAsset> update(
            @PathVariable String projectId,
            @PathVariable String assetId,
            @RequestBody UpdateAssetRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.updateAsset(projectId, assetId, request));
    }
}
