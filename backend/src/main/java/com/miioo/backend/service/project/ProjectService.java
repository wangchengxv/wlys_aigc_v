package com.miioo.backend.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miioo.backend.common.BizException;
import com.miioo.backend.project.ProjectEntity;
import com.miioo.backend.project.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    public List<Map<String, Object>> list(Long userId) {
        return projectMapper.selectList(new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getUserId, userId)
                        .orderByDesc(ProjectEntity::getId))
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Long userId, String name, String description) {
        Long count = projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getUserId, userId)
                .eq(ProjectEntity::getName, name));
        if (count != null && count > 0) {
            throw new BizException(400, "项目名称重复");
        }
        ProjectEntity project = new ProjectEntity();
        project.setUserId(userId);
        project.setName(name);
        project.setDescription(description);
        projectMapper.insert(project);
        return toMap(project);
    }

    public Map<String, Object> detail(Long userId, Long id) {
        return toMap(getOwnedProject(userId, id));
    }

    @Transactional
    public Map<String, Object> update(Long userId, Long id, String name, String description) {
        ProjectEntity item = getOwnedProject(userId, id);
        item.setName(name);
        item.setDescription(description);
        projectMapper.updateById(item);
        return toMap(item);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        getOwnedProject(userId, id);
        projectMapper.deleteById(id);
    }

    private ProjectEntity getOwnedProject(Long userId, Long id) {
        ProjectEntity item = projectMapper.selectById(id);
        if (item == null) {
            throw new BizException(404, "项目不存在");
        }
        if (!userId.equals(item.getUserId())) {
            throw new BizException(403, "无权访问该项目");
        }
        return item;
    }

    private Map<String, Object> toMap(ProjectEntity item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("name", item.getName());
        result.put("description", item.getDescription());
        return result;
    }
}
