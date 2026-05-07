package com.miioo.backend.aitask;

import com.miioo.backend.common.ApiResponse;
import com.miioo.backend.common.SecurityUtils;
import com.miioo.backend.service.aitask.AiTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-tasks")
public class AiTaskController {
    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long taskId) {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(aiTaskService.getTask(userId, taskId));
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myTasks() {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(aiTaskService.myTasks(userId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long taskId) {
        Long userId = SecurityUtils.currentUserId();
        aiTaskService.cancel(userId, taskId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{taskId}/retry")
    public ApiResponse<Map<String, Object>> retry(@PathVariable Long taskId) {
        Long userId = SecurityUtils.currentUserId();
        Long newTaskId = aiTaskService.retry(userId, taskId);
        return ApiResponse.success(Map.of("taskId", newTaskId));
    }
}
