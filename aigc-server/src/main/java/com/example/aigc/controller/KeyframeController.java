package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.RollbackPromptRequest;
import com.example.aigc.dto.UpdateKeyframePromptRequest;
import com.example.aigc.entity.KeyframeRecord;
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
@RequestMapping("/api/v1/script-projects/{projectId}")
public class KeyframeController {

    private final ScriptWorkflowService scriptWorkflowService;

    public KeyframeController(ScriptWorkflowService scriptWorkflowService) {
        this.scriptWorkflowService = scriptWorkflowService;
    }

    @PostMapping("/assets/{assetId}/keyframes/generate")
    public ApiResponse<List<KeyframeRecord>> generate(
            @PathVariable String projectId,
            @PathVariable String assetId
    ) {
        return ApiResponse.ok(scriptWorkflowService.generateKeyframes(projectId, assetId));
    }

    @GetMapping("/keyframes")
    public ApiResponse<List<KeyframeRecord>> list(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.listKeyframes(projectId));
    }

    @PostMapping("/keyframes/{keyframeId}/confirm")
    public ApiResponse<KeyframeRecord> confirm(
            @PathVariable String projectId,
            @PathVariable String keyframeId
    ) {
        return ApiResponse.ok(scriptWorkflowService.confirmKeyframe(projectId, keyframeId));
    }

    @PostMapping("/keyframes/{keyframeId}/regenerate")
    public ApiResponse<List<KeyframeRecord>> regenerate(
            @PathVariable String projectId,
            @PathVariable String keyframeId
    ) {
        return ApiResponse.ok(scriptWorkflowService.regenerateKeyframe(projectId, keyframeId));
    }

    @PutMapping("/keyframes/{keyframeId}/prompt")
    public ApiResponse<KeyframeRecord> updatePrompt(
            @PathVariable String projectId,
            @PathVariable String keyframeId,
            @RequestBody UpdateKeyframePromptRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.updateKeyframePrompt(projectId, keyframeId, request));
    }

    @PostMapping("/keyframes/{keyframeId}/prompt/rollback")
    public ApiResponse<KeyframeRecord> rollbackPrompt(
            @PathVariable String projectId,
            @PathVariable String keyframeId,
            @RequestBody RollbackPromptRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.rollbackKeyframePrompt(projectId, keyframeId, request));
    }
}
