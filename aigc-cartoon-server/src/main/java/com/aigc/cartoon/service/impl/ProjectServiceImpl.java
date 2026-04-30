package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.Project;
import com.aigc.cartoon.mapper.ProjectMapper;
import com.aigc.cartoon.service.ProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectMapper projectMapper;
    
    public ProjectServiceImpl(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }
    
    @Override
    public List<Project> list() {
        return projectMapper.selectList(null);
    }
    
    @Override
    public List<Project> listByUserId(Long userId) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getUserId, userId);
        wrapper.orderByDesc(Project::getCreateTime);
        return projectMapper.selectList(wrapper);
    }
    
    @Override
    public Project getById(Long id) {
        return projectMapper.selectById(id);
    }
    
    @Override
    public boolean save(Project project) {
        if (project.getStatus() == null) {
            project.setStatus("draft");
        }
        return projectMapper.insert(project) > 0;
    }
    
    @Override
    public boolean updateById(Project project) {
        return projectMapper.updateById(project) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return projectMapper.deleteById(id) > 0;
    }
    
    @Override
    public boolean rename(Long id, String newName) {
        Project project = new Project();
        project.setId(id);
        project.setName(newName);
        return projectMapper.updateById(project) > 0;
    }
}