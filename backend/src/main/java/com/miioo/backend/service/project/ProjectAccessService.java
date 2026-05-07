package com.miioo.backend.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miioo.backend.common.BizException;
import com.miioo.backend.project.ProjectEntity;
import com.miioo.backend.project.ProjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {
    private final ProjectMapper projectMapper;

    public ProjectAccessService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    public void checkOwner(Long userId, Long projectId) {
        Long count = projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getId, projectId)
                .eq(ProjectEntity::getUserId, userId));
        if (count == null || count == 0L) {
            throw new BizException(403, "无权访问该项目");
        }
    }
}
