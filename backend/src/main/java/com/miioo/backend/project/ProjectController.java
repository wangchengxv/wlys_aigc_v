package com.miioo.backend.project;

import com.miioo.backend.common.ApiResponse;
import com.miioo.backend.common.SecurityUtils;
import com.miioo.backend.service.project.ProjectService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@Validated
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(projectService.list(userId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateProjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(projectService.create(userId, request.name(), request.description()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(projectService.detail(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody CreateProjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(projectService.update(userId, id, request.name(), request.description()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        projectService.delete(userId, id);
        return ApiResponse.success(null);
    }

    public record CreateProjectRequest(@NotBlank String name, String description) {}
}
