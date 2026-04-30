package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.Script;
import java.util.List;

public interface ScriptService {
    
    List<Script> listByProjectId(Long projectId);
    
    Script getById(Long id);
    
    boolean save(Script script);
    
    boolean updateById(Script script);
    
    boolean removeById(Long id);
    
    Script getLatestByProjectId(Long projectId);
}