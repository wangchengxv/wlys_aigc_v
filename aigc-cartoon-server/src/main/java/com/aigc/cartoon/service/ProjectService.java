package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.Project;
import java.util.List;

public interface ProjectService {
    
    List<Project> list();
    
    List<Project> listByUserId(Long userId);
    
    Project getById(Long id);
    
    boolean save(Project project);
    
    boolean updateById(Project project);
    
    boolean removeById(Long id);
    
    boolean rename(Long id, String newName);
}