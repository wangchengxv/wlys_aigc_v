package com.miioo.backend.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miioo.backend.common.ApiResponse;
import com.miioo.backend.common.BizException;
import com.miioo.backend.common.SecurityUtils;
import com.miioo.backend.project.ProjectEntity;
import com.miioo.backend.project.ProjectMapper;
import com.miioo.backend.service.orchestration.OrchestrationService;
import com.miioo.backend.service.project.ProjectAccessService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@Validated
public class AssetController {
    private final AssetMapper assetMapper;
    private final ProjectMapper projectMapper;
    private final OrchestrationService orchestrationService;
    private final ProjectAccessService projectAccessService;

    public AssetController(AssetMapper assetMapper,
                           ProjectMapper projectMapper,
                           OrchestrationService orchestrationService,
                           ProjectAccessService projectAccessService) {
        this.assetMapper = assetMapper;
        this.projectMapper = projectMapper;
        this.orchestrationService = orchestrationService;
        this.projectAccessService = projectAccessService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        Long userId = SecurityUtils.currentUserId();
        List<Long> projectIds = projectMapper.selectList(new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getUserId, userId)
                        .select(ProjectEntity::getId))
                .stream()
                .map(ProjectEntity::getId)
                .toList();
        if (projectIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<Map<String, Object>> items = assetMapper.selectList(new LambdaQueryWrapper<AssetEntity>()
                        .in(AssetEntity::getProjectId, projectIds)
                        .orderByDesc(AssetEntity::getId))
                .stream()
                .map(this::toMap)
                .toList();
        return ApiResponse.success(items);
    }

    @PostMapping("/generate-image")
    public ApiResponse<Map<String, Object>> generateImage(@RequestBody GenerateImageRequest request) {
        Long userId = SecurityUtils.currentUserId();
        projectAccessService.checkOwner(userId, request.projectId());
        Long taskId = orchestrationService.generateImageTask(userId, request.projectId(), request.prompt());
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestBody UploadAssetRequest request) {
        Long userId = SecurityUtils.currentUserId();
        projectAccessService.checkOwner(userId, request.projectId());
        AssetEntity item = new AssetEntity();
        item.setProjectId(request.projectId());
        item.setAssetType(request.assetType());
        item.setUrl(request.url());
        item.setStarred(Boolean.FALSE);
        assetMapper.insert(item);
        return ApiResponse.success(toMap(item));
    }

    @PostMapping("/{id}/star")
    public ApiResponse<Map<String, Object>> star(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        AssetEntity item = assetMapper.selectById(id);
        if (item == null) throw new BizException(404, "资产不存在");
        projectAccessService.checkOwner(userId, item.getProjectId());
        item.setStarred(Boolean.TRUE);
        assetMapper.updateById(item);
        return ApiResponse.success(toMap(item));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        AssetEntity item = assetMapper.selectById(id);
        if (item == null) throw new BizException(404, "资产不存在");
        projectAccessService.checkOwner(userId, item.getProjectId());
        assetMapper.deleteById(id);
        return ApiResponse.success(null);
    }

    private Map<String, Object> toMap(AssetEntity item) {
        return Map.of(
                "id", item.getId(),
                "projectId", item.getProjectId(),
                "assetType", item.getAssetType() == null ? "" : item.getAssetType(),
                "url", item.getUrl() == null ? "" : item.getUrl(),
                "starred", item.getStarred() != null && item.getStarred()
        );
    }

    public record GenerateImageRequest(@NotNull Long projectId, @NotBlank String prompt) {}

    public record UploadAssetRequest(@NotNull Long projectId, @NotBlank String assetType, @NotBlank String url) {}
}
