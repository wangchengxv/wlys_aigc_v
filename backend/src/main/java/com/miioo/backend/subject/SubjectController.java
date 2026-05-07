package com.miioo.backend.subject;

import com.miioo.backend.common.ApiResponse;
import com.miioo.backend.common.SecurityUtils;
import com.miioo.backend.service.subject.SubjectService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/subjects")
@Validated
public class SubjectController {
    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(subjectService.list(userId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateSubjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(subjectService.create(userId, request.projectId(), request.name(), request.subjectType()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody CreateSubjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(subjectService.update(userId, id, request.name(), request.subjectType()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        subjectService.delete(userId, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody GenerateSubjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        Long taskId = subjectService.generate(userId, request.projectId(), request.sourceText());
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    @PostMapping("/{id}/finalize")
    public ApiResponse<Map<String, Object>> finalizeSubject(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(subjectService.finalizeSubject(userId, id));
    }

    public record CreateSubjectRequest(@NotNull Long projectId, @NotBlank String name, @NotBlank String subjectType) {}

    public record GenerateSubjectRequest(@NotNull Long projectId, @NotBlank String sourceText) {}
}
