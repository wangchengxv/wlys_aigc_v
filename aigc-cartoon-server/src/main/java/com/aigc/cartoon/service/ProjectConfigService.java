package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.ProjectConfig;

public interface ProjectConfigService {
    
    ProjectConfig getByProjectId(Long projectId);
    
    boolean save(ProjectConfig config);
    
    boolean update(ProjectConfig config);
}