package com.miioo.backend.service.aitask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miioo.backend.aitask.AiTaskEntity;
import com.miioo.backend.aitask.AiTaskMapper;
import com.miioo.backend.common.BizException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiTaskService {
    private final AiTaskMapper aiTaskMapper;

    public AiTaskService(AiTaskMapper aiTaskMapper) {
        this.aiTaskMapper = aiTaskMapper;
    }

    public Long createTask(Long userId, String taskType, Long projectId) {
        AiTaskEntity task = new AiTaskEntity();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setProjectId(projectId);
        task.setStatus("PENDING");
        task.setProgress(0);
        aiTaskMapper.insert(task);
        return task.getId();
    }

    public Map<String, Object> getTask(Long userId, Long taskId) {
        AiTaskEntity task = getOwnedTask(userId, taskId);
        return toMap(task);
    }

    public List<Map<String, Object>> myTasks(Long userId) {
        return aiTaskMapper.selectList(new LambdaQueryWrapper<AiTaskEntity>()
                        .eq(AiTaskEntity::getUserId, userId)
                        .orderByDesc(AiTaskEntity::getId))
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Async
    public void runMockTask(Long taskId, Runnable onSuccess) {
        AiTaskEntity task = aiTaskMapper.selectById(taskId);
        if (task == null || "CANCELLED".equals(task.getStatus())) {
            return;
        }
        task.setStatus("RUNNING");
        task.setProgress(30);
        aiTaskMapper.updateById(task);
        try {
            Thread.sleep(500);
            task = aiTaskMapper.selectById(taskId);
            if (task == null || "CANCELLED".equals(task.getStatus())) {
                return;
            }
            task.setProgress(70);
            aiTaskMapper.updateById(task);
            Thread.sleep(500);
            onSuccess.run();
            task = aiTaskMapper.selectById(taskId);
            if (task == null || "CANCELLED".equals(task.getStatus())) {
                return;
            }
            task.setStatus("SUCCESS");
            task.setProgress(100);
            aiTaskMapper.updateById(task);
        } catch (Exception e) {
            AiTaskEntity failedTask = aiTaskMapper.selectById(taskId);
            if (failedTask != null && !"CANCELLED".equals(failedTask.getStatus())) {
                failedTask.setStatus("FAILED");
                failedTask.setErrorMessage(e.getMessage());
                aiTaskMapper.updateById(failedTask);
            }
        }
    }

    public void cancel(Long userId, Long taskId) {
        AiTaskEntity task = getOwnedTask(userId, taskId);
        task.setStatus("CANCELLED");
        aiTaskMapper.updateById(task);
    }

    public Long retry(Long userId, Long taskId) {
        AiTaskEntity task = getOwnedTask(userId, taskId);
        return createTask(userId, task.getTaskType(), task.getProjectId());
    }

    private AiTaskEntity getOwnedTask(Long userId, Long taskId) {
        AiTaskEntity task = aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTaskEntity>()
                .eq(AiTaskEntity::getId, taskId)
                .eq(AiTaskEntity::getUserId, userId));
        if (task == null) throw new BizException(404, "任务不存在");
        return task;
    }

    private Map<String, Object> toMap(AiTaskEntity task) {
        return Map.of(
                "id", task.getId(),
                "userId", task.getUserId(),
                "projectId", task.getProjectId(),
                "taskType", task.getTaskType() == null ? "" : task.getTaskType(),
                "status", task.getStatus() == null ? "" : task.getStatus(),
                "progress", task.getProgress() == null ? 0 : task.getProgress(),
                "errorMessage", task.getErrorMessage() == null ? "" : task.getErrorMessage()
        );
    }
}
