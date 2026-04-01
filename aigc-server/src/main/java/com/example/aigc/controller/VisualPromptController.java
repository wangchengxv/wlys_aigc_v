package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ApplyStoryboardFirstFrameRequest;
import com.example.aigc.dto.ArtDirectionResponse;
import com.example.aigc.dto.BatchVisualPromptResponse;
import com.example.aigc.dto.GenerateGroupSceneRequest;
import com.example.aigc.dto.GroupSceneResponse;
import com.example.aigc.dto.ShotVisualPromptResponse;
import com.example.aigc.dto.StoryboardFirstFrameResponse;
import com.example.aigc.dto.StoryboardImageResponse;
import com.example.aigc.dto.StoryboardPanelCropResponse;
import com.example.aigc.dto.StoryboardPlanResponse;
import com.example.aigc.dto.StoryboardRewriteRequest;
import com.example.aigc.dto.ThreeViewResponse;
import com.example.aigc.dto.TurnaroundImageResponse;
import com.example.aigc.dto.TurnaroundPlanResponse;
import com.example.aigc.dto.VisualPromptResponse;
import com.example.aigc.service.ScriptWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/script-projects/{projectId}")
public class VisualPromptController {

    private final ScriptWorkflowService scriptWorkflowService;

    public VisualPromptController(ScriptWorkflowService scriptWorkflowService) {
        this.scriptWorkflowService = scriptWorkflowService;
    }

    @PostMapping("/art-direction/generate")
    public ApiResponse<ArtDirectionResponse> generateArtDirection(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.generateArtDirection(projectId));
    }

    @GetMapping("/art-direction")
    public ApiResponse<ArtDirectionResponse> getArtDirection(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.getArtDirection(projectId));
    }

    @PostMapping("/assets/visual-prompt/batch-generate")
    public ApiResponse<BatchVisualPromptResponse> batchGenerateCharacterVisualPrompts(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.batchGenerateCharacterVisualPrompts(projectId));
    }

    @PostMapping("/assets/{assetId}/visual-prompt/generate")
    public ApiResponse<VisualPromptResponse> generateAssetVisualPrompt(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateAssetVisualPrompt(projectId, assetId));
    }

    @PostMapping("/assets/{assetId}/turnaround/plan")
    public ApiResponse<TurnaroundPlanResponse> generateTurnaroundPlan(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateTurnaroundPlan(projectId, assetId));
    }

    @PostMapping("/assets/{assetId}/turnaround/generate")
    public ApiResponse<TurnaroundImageResponse> generateTurnaroundImage(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateTurnaroundImage(projectId, assetId));
    }

    @PostMapping("/assets/{assetId}/storyboard/plan")
    public ApiResponse<StoryboardPlanResponse> generateStoryboardPlan(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateStoryboardPlan(projectId, assetId));
    }

    @PostMapping("/assets/{assetId}/storyboard/translate")
    public ApiResponse<StoryboardPlanResponse> translateStoryboardPlan(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.translateStoryboardPlan(projectId, assetId));
    }

    @PostMapping("/assets/{assetId}/storyboard/rewrite")
    public ApiResponse<StoryboardPlanResponse> rewriteStoryboardPlan(
            @PathVariable String projectId,
            @PathVariable String assetId,
            @Valid @RequestBody StoryboardRewriteRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.rewriteStoryboardPlan(projectId, assetId, request));
    }

    @PostMapping("/assets/{assetId}/storyboard/image")
    public ApiResponse<StoryboardImageResponse> generateStoryboardImage(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateStoryboardImage(projectId, assetId));
    }

    @PostMapping("/assets/{assetId}/storyboard/panels/{panelIndex}/crop")
    public ApiResponse<StoryboardPanelCropResponse> cropStoryboardPanel(
            @PathVariable String projectId,
            @PathVariable String assetId,
            @PathVariable int panelIndex
    ) {
        return ApiResponse.ok(scriptWorkflowService.cropStoryboardPanel(projectId, assetId, panelIndex));
    }

    @PostMapping("/assets/{assetId}/three-view/generate")
    public ApiResponse<ThreeViewResponse> generateThreeView(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateThreeView(projectId, assetId));
    }

    @PostMapping("/visual/group-scene")
    public ApiResponse<GroupSceneResponse> generateGroupScene(
            @PathVariable String projectId,
            @Valid @RequestBody GenerateGroupSceneRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateGroupScene(projectId, request));
    }

    @PostMapping("/shots/{shotId}/visual-prompt/generate")
    public ApiResponse<ShotVisualPromptResponse> generateShotVisualPrompt(
            @PathVariable String projectId,
            @PathVariable String shotId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateShotVisualPrompt(projectId, shotId));
    }

    @PostMapping("/shots/{shotId}/storyboard-first-frame/apply")
    public ApiResponse<StoryboardFirstFrameResponse> applyStoryboardFirstFrame(
            @PathVariable String projectId,
            @PathVariable String shotId,
            @RequestBody(required = false) ApplyStoryboardFirstFrameRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.applyStoryboardFirstFrame(projectId, shotId, request));
    }
}
