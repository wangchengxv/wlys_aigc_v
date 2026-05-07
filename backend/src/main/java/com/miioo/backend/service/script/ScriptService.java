package com.miioo.backend.service.script;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miioo.backend.project.ProjectEntity;
import com.miioo.backend.project.ProjectMapper;
import com.miioo.backend.script.ScriptEntity;
import com.miioo.backend.script.ScriptMapper;
import com.miioo.backend.service.orchestration.OrchestrationService;
import com.miioo.backend.service.project.ProjectAccessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ScriptService {
    private final ScriptMapper scriptMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService projectAccessService;
    private final OrchestrationService orchestrationService;

    public ScriptService(ScriptMapper scriptMapper,
                         ProjectMapper projectMapper,
                         ProjectAccessService projectAccessService,
                         OrchestrationService orchestrationService) {
        this.scriptMapper = scriptMapper;
        this.projectMapper = projectMapper;
        this.projectAccessService = projectAccessService;
        this.orchestrationService = orchestrationService;
    }

    public Long generate(Long userId, Long projectId, String prompt) {
        projectAccessService.checkOwner(userId, projectId);
        return orchestrationService.generateScriptTask(userId, projectId, prompt);
    }

    public Long upload(Long userId, Long projectId, String content) {
        projectAccessService.checkOwner(userId, projectId);
        return orchestrationService.generateScriptTask(userId, projectId, content);
    }

    public Long extractSubjects(Long userId, Long projectId, String content) {
        projectAccessService.checkOwner(userId, projectId);
        return orchestrationService.extractSubjectTask(userId, projectId, content);
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Long> projectIds = projectMapper.selectList(new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getUserId, userId)
                        .select(ProjectEntity::getId))
                .stream()
                .map(ProjectEntity::getId)
                .toList();
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return scriptMapper.selectList(new LambdaQueryWrapper<ScriptEntity>()
                        .in(ScriptEntity::getProjectId, projectIds)
                        .orderByDesc(ScriptEntity::getId))
                .stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.getId(),
                        "projectId", item.getProjectId(),
                        "title", item.getTitle() == null ? "" : item.getTitle(),
                        "content", item.getContent() == null ? "" : item.getContent()))
                .toList();
    }
}
