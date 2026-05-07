package com.miioo.backend.service.subject;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miioo.backend.common.BizException;
import com.miioo.backend.project.ProjectEntity;
import com.miioo.backend.project.ProjectMapper;
import com.miioo.backend.service.orchestration.OrchestrationService;
import com.miioo.backend.service.project.ProjectAccessService;
import com.miioo.backend.subject.SubjectEntity;
import com.miioo.backend.subject.SubjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SubjectService {
    private final SubjectMapper subjectMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService projectAccessService;
    private final OrchestrationService orchestrationService;

    public SubjectService(SubjectMapper subjectMapper,
                          ProjectMapper projectMapper,
                          ProjectAccessService projectAccessService,
                          OrchestrationService orchestrationService) {
        this.subjectMapper = subjectMapper;
        this.projectMapper = projectMapper;
        this.projectAccessService = projectAccessService;
        this.orchestrationService = orchestrationService;
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
        return subjectMapper.selectList(new LambdaQueryWrapper<SubjectEntity>()
                        .in(SubjectEntity::getProjectId, projectIds)
                        .orderByDesc(SubjectEntity::getId))
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Long userId, Long projectId, String name, String subjectType) {
        projectAccessService.checkOwner(userId, projectId);
        SubjectEntity item = new SubjectEntity();
        item.setProjectId(projectId);
        item.setName(name);
        item.setSubjectType(subjectType);
        item.setFinalized(Boolean.FALSE);
        subjectMapper.insert(item);
        return toMap(item);
    }

    @Transactional
    public Map<String, Object> update(Long userId, Long id, String name, String subjectType) {
        SubjectEntity item = getOwnedSubject(userId, id);
        item.setName(name);
        item.setSubjectType(subjectType);
        subjectMapper.updateById(item);
        return toMap(item);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        getOwnedSubject(userId, id);
        subjectMapper.deleteById(id);
    }

    public Long generate(Long userId, Long projectId, String sourceText) {
        projectAccessService.checkOwner(userId, projectId);
        return orchestrationService.extractSubjectTask(userId, projectId, sourceText);
    }

    @Transactional
    public Map<String, Object> finalizeSubject(Long userId, Long id) {
        SubjectEntity item = getOwnedSubject(userId, id);
        item.setFinalized(Boolean.TRUE);
        subjectMapper.updateById(item);
        return toMap(item);
    }

    private SubjectEntity getOwnedSubject(Long userId, Long id) {
        SubjectEntity item = subjectMapper.selectById(id);
        if (item == null) throw new BizException(404, "主体不存在");
        projectAccessService.checkOwner(userId, item.getProjectId());
        return item;
    }

    private Map<String, Object> toMap(SubjectEntity item) {
        return Map.of(
                "id", item.getId(),
                "projectId", item.getProjectId(),
                "name", item.getName() == null ? "" : item.getName(),
                "subjectType", item.getSubjectType() == null ? "" : item.getSubjectType(),
                "finalized", item.getFinalized() != null && item.getFinalized()
        );
    }
}
