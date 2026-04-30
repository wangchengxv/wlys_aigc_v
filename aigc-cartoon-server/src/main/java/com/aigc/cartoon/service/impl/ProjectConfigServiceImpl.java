package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.ProjectConfig;
import com.aigc.cartoon.mapper.ProjectConfigMapper;
import com.aigc.cartoon.service.ProjectConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class ProjectConfigServiceImpl implements ProjectConfigService {
    
    private final ProjectConfigMapper configMapper;
    
    public ProjectConfigServiceImpl(ProjectConfigMapper configMapper) {
        this.configMapper = configMapper;
    }
    
    @Override
    public ProjectConfig getByProjectId(Long projectId) {
        LambdaQueryWrapper<ProjectConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectConfig::getProjectId, projectId);
        return configMapper.selectOne(wrapper);
    }
    
    @Override
    public boolean save(ProjectConfig config) {
        return configMapper.insert(config) > 0;
    }
    
    @Override
    public boolean update(ProjectConfig config) {
        return configMapper.updateById(config) > 0;
    }
}