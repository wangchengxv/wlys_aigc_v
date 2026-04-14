package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.AssetGenerationHistoryItem;
import com.example.aigc.entity.AssetGenerationHistory;
import com.example.aigc.enums.AssetHistoryType;
import com.example.aigc.exception.BizException;
import com.example.aigc.service.AssetHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/script-projects/{projectId}/asset-history")
public class AssetHistoryController {

    private final AssetHistoryService assetHistoryService;

    public AssetHistoryController(AssetHistoryService assetHistoryService) {
        this.assetHistoryService = assetHistoryService;
    }

    @GetMapping
    public ApiResponse<List<AssetGenerationHistoryItem>> list(
            @PathVariable String projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String referenceId
    ) {
        AssetHistoryType assetType = null;
        if (type != null && !type.isBlank()) {
            try {
                assetType = AssetHistoryType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BizException(400, "无效的 asset type: " + type);
            }
        }
        List<AssetGenerationHistory> rows = assetHistoryService.list(projectId, assetType, referenceId);
        return ApiResponse.ok(rows.stream().map(this::toItem).toList());
    }

    @PostMapping("/{historyId}/restore")
    public ApiResponse<AssetGenerationHistoryItem> restore(
            @PathVariable String projectId,
            @PathVariable long historyId
    ) {
        AssetGenerationHistory restored = assetHistoryService.restore(projectId, historyId);
        return ApiResponse.ok(toItem(restored));
    }

    private AssetGenerationHistoryItem toItem(AssetGenerationHistory h) {
        return new AssetGenerationHistoryItem(
                h.getId(),
                h.getProjectId(),
                h.getAssetType().name(),
                h.getReferenceId(),
                h.getFileId(),
                h.getPromptText(),
                h.getModelName(),
                h.getGenerationParamsJson(),
                h.getCreatedAt()
        );
    }
}
