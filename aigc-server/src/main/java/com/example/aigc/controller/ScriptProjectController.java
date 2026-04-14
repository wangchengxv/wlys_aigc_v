package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.AppendScriptPreviewRequest;
import com.example.aigc.dto.AppendScriptPreviewResponse;
import com.example.aigc.dto.ScriptDocumentPayload;
import com.example.aigc.dto.ScriptProjectCreateRequest;
import com.example.aigc.dto.RefineScriptWithPromptRequest;
import com.example.aigc.dto.RewriteScriptApplyRequest;
import com.example.aigc.dto.RewriteScriptPreviewRequest;
import com.example.aigc.dto.RewriteScriptPreviewResponse;
import com.example.aigc.dto.UpdateScriptRequest;
import com.example.aigc.dto.PromptTemplateOverridesUpdateRequest;
import com.example.aigc.dto.WorkflowModelSettingsResponse;
import com.example.aigc.dto.WorkflowModelSettingsUpdateRequest;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.entity.ScriptRevision;
import com.example.aigc.service.ScriptProjectService;
import com.example.aigc.service.ScriptWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/script-projects")
public class ScriptProjectController {

    private final ScriptProjectService scriptProjectService;
    private final ScriptWorkflowService scriptWorkflowService;

    public ScriptProjectController(
            ScriptProjectService scriptProjectService,
            ScriptWorkflowService scriptWorkflowService
    ) {
        this.scriptProjectService = scriptProjectService;
        this.scriptWorkflowService = scriptWorkflowService;
    }

    @PostMapping
    public ApiResponse<ScriptProjectAggregate> create(@Valid @RequestBody ScriptProjectCreateRequest request) {
        return ApiResponse.ok(scriptProjectService.create(request));
    }

    @PostMapping("/upload")
    public ApiResponse<ScriptProjectAggregate> upload(
            @RequestParam String name,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String visualStyle,
            @RequestParam(required = false) String aspectRatio,
            @RequestParam(required = false) Integer targetDuration,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String explicitTextModel,
            @RequestParam(required = false) String explicitImageModel,
            @RequestParam(required = false) String explicitVideoModel
    ) {
        return ApiResponse.ok(scriptProjectService.createFromUpload(
                name,
                file,
                visualStyle,
                aspectRatio,
                targetDuration,
                language,
                explicitTextModel,
                explicitImageModel,
                explicitVideoModel
        ));
    }

    @GetMapping
    public ApiResponse<List<ScriptProjectSummary>> list(
            @RequestParam(name = "deleted", required = false, defaultValue = "false") boolean deleted
    ) {
        return ApiResponse.ok(scriptProjectService.list(deleted));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ScriptProjectAggregate> detail(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProjectService.require(projectId));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable String projectId) {
        scriptProjectService.delete(projectId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{projectId}/restore")
    public ApiResponse<ScriptProjectAggregate> restore(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProjectService.restore(projectId));
    }

    @PostMapping("/{projectId}/refine")
    public ApiResponse<ScriptDocumentPayload> refine(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.refineScript(projectId));
    }

    @PostMapping("/{projectId}/refine-with-brief")
    public ApiResponse<ScriptDocumentPayload> refineWithBrief(
            @PathVariable String projectId,
            @Valid @RequestBody RefineScriptWithPromptRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.refineScriptWithBrief(projectId, request.briefPrompt()));
    }

    @PostMapping("/{projectId}/rewrite/preview")
    public ApiResponse<RewriteScriptPreviewResponse> rewritePreview(
            @PathVariable String projectId,
            @Valid @RequestBody RewriteScriptPreviewRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.rewriteScriptPreview(projectId, request));
    }

    @PostMapping("/{projectId}/rewrite/apply")
    public ApiResponse<ScriptDocumentPayload> rewriteApply(
            @PathVariable String projectId,
            @Valid @RequestBody RewriteScriptApplyRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.applyRewrite(projectId, request));
    }

    @GetMapping("/{projectId}/script")
    public ApiResponse<ScriptDocumentPayload> script(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProjectService.getScriptPayload(projectId));
    }

    @PutMapping("/{projectId}/script")
    public ApiResponse<ScriptDocumentPayload> updateScript(
            @PathVariable String projectId,
            @RequestBody UpdateScriptRequest request
    ) {
        return ApiResponse.ok(scriptProjectService.updateScript(projectId, request));
    }

    @PostMapping("/{projectId}/import")
    public ApiResponse<ScriptProjectAggregate> importScript(
            @PathVariable String projectId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String replaceName,
            @RequestParam(required = false, defaultValue = "false") boolean autoRefine
    ) {
        scriptProjectService.importScript(projectId, file, replaceName);
        if (autoRefine) {
            scriptWorkflowService.refineScript(projectId);
        }
        return ApiResponse.ok(scriptProjectService.require(projectId));
    }

    @GetMapping("/{projectId}/revisions")
    public ApiResponse<List<ScriptRevision>> listRevisions(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProjectService.listRevisions(projectId));
    }

    @PostMapping("/{projectId}/revisions/{revisionId}/restore")
    public ApiResponse<ScriptDocumentPayload> restoreRevision(
            @PathVariable String projectId,
            @PathVariable String revisionId
    ) {
        return ApiResponse.ok(scriptProjectService.restoreRevision(projectId, revisionId));
    }

    @PostMapping("/{projectId}/optimize/scenes")
    public ApiResponse<ScriptDocumentPayload> optimizeScenes(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.optimizeScenes(projectId));
    }

    @PostMapping("/{projectId}/optimize/characters")
    public ApiResponse<ScriptDocumentPayload> optimizeCharacters(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.optimizeCharacters(projectId));
    }

    @PostMapping("/{projectId}/optimize/props")
    public ApiResponse<ScriptDocumentPayload> optimizeProps(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.optimizeProps(projectId));
    }

    @PostMapping("/{projectId}/append/preview")
    public ApiResponse<AppendScriptPreviewResponse> appendPreview(
            @PathVariable String projectId,
            @Valid @RequestBody(required = false) AppendScriptPreviewRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.appendScriptPreview(projectId, request));
    }

    @GetMapping("/{projectId}/model-settings")
    public ApiResponse<WorkflowModelSettingsResponse> getModelSettings(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProjectService.getModelSettings(projectId));
    }

    @PutMapping("/{projectId}/model-settings")
    public ApiResponse<WorkflowModelSettingsResponse> updateModelSettings(
            @PathVariable String projectId,
            @RequestBody WorkflowModelSettingsUpdateRequest request
    ) {
        return ApiResponse.ok(scriptProjectService.updateModelSettings(projectId, request));
    }

    @GetMapping("/{projectId}/prompt-template-overrides")
    public ApiResponse<Map<String, String>> getPromptTemplateOverrides(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProjectService.getPromptTemplateOverrides(projectId));
    }

    @PutMapping("/{projectId}/prompt-template-overrides")
    public ApiResponse<Map<String, String>> updatePromptTemplateOverrides(
            @PathVariable String projectId,
            @RequestBody PromptTemplateOverridesUpdateRequest request
    ) {
        return ApiResponse.ok(scriptProjectService.updatePromptTemplateOverrides(projectId, request));
    }
}
