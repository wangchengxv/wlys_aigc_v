package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.PipelineStatusData;
import com.example.aigc.dto.RollbackPromptRequest;
import com.example.aigc.dto.UpdateShotRequest;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.service.ScriptProductionOrchestrator;
import com.example.aigc.service.ScriptWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/script-projects/{projectId}")
public class VideoPipelineController {

    private final ScriptWorkflowService scriptWorkflowService;
    private final ScriptProductionOrchestrator scriptProductionOrchestrator;

    public VideoPipelineController(
            ScriptWorkflowService scriptWorkflowService,
            ScriptProductionOrchestrator scriptProductionOrchestrator
    ) {
        this.scriptWorkflowService = scriptWorkflowService;
        this.scriptProductionOrchestrator = scriptProductionOrchestrator;
    }

    @PostMapping("/shots/split")
    public ApiResponse<List<StoryboardShot>> split(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.splitShots(projectId));
    }

    @GetMapping("/shots")
    public ApiResponse<List<StoryboardShot>> shots(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.listShots(projectId));
    }

    @PutMapping("/shots/{shotId}")
    public ApiResponse<StoryboardShot> updateShot(
            @PathVariable String projectId,
            @PathVariable String shotId,
            @Valid @RequestBody UpdateShotRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.updateShot(projectId, shotId, request));
    }

    @PostMapping("/shots/{shotId}/visual-prompt/rollback")
    public ApiResponse<StoryboardShot> rollbackShotVisualPrompt(
            @PathVariable String projectId,
            @PathVariable String shotId,
            @RequestBody RollbackPromptRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.rollbackShotVisualPrompt(projectId, shotId, request));
    }

    @PostMapping("/video/generate")
    public ApiResponse<PipelineStatusData> generateVideo(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.startVideoGeneration(projectId));
    }

    @GetMapping("/video/tasks")
    public ApiResponse<List<VideoSegmentTask>> tasks(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.listVideoTasks(projectId));
    }

    @PostMapping("/video/tasks/{segmentTaskId}/retry")
    public ApiResponse<PipelineStatusData> retry(
            @PathVariable String projectId,
            @PathVariable String segmentTaskId
    ) {
        return ApiResponse.ok(scriptProductionOrchestrator.retryVideoTask(projectId, segmentTaskId));
    }

    @GetMapping("/pipeline-status")
    public ApiResponse<PipelineStatusData> pipelineStatus(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.getPipelineStatus(projectId));
    }
}
