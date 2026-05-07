package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ScriptDocumentPayload;
import com.example.aigc.dto.ScriptProjectCreateRequest;
import com.example.aigc.dto.RefineScriptWithPromptRequest;
import com.example.aigc.dto.UpdateScriptRequest;
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

@RestController
@RequestMapping("/api/v1/script-projects")
public class ScriptProjectController {

    private final ScriptProjectService scriptProjectService;
    private final ScriptWorkflowService scriptWorkflowService;

    public ScriptProjectController(ScriptProjectService scriptProjectService, ScriptWorkflowService scriptWorkflowService) {
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
    public ApiResponse<List<ScriptProjectSummary>> list() {
        return ApiResponse.ok(scriptProjectService.list());
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
}
