package com.miioo.backend.script;

import com.miioo.backend.common.ApiResponse;
import com.miioo.backend.common.SecurityUtils;
import com.miioo.backend.service.script.ScriptService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scripts")
@Validated
public class ScriptController {
    private final ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody GenerateScriptRequest request) {
        Long userId = SecurityUtils.currentUserId();
        Long taskId = scriptService.generate(userId, request.projectId(), request.prompt());
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestBody UploadScriptRequest request) {
        Long userId = SecurityUtils.currentUserId();
        Long taskId = scriptService.upload(userId, request.projectId(), request.content());
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    @PostMapping("/extract-subjects")
    public ApiResponse<Map<String, Object>> extractSubjects(@RequestBody ExtractSubjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        Long taskId = scriptService.extractSubjects(userId, request.projectId(), request.content());
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(scriptService.list(userId));
    }

    public record GenerateScriptRequest(@NotNull Long projectId, @NotBlank String prompt) {}

    public record UploadScriptRequest(@NotNull Long projectId, @NotBlank String content) {}

    public record ExtractSubjectRequest(@NotNull Long projectId, @NotBlank String content) {}
}
