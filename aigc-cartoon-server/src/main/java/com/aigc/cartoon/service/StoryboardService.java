package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.Storyboard;
import java.util.List;

public interface StoryboardService {
    
    List<Storyboard> listByScriptId(Long scriptId);
    
    Storyboard getById(Long id);
    
    boolean save(Storyboard storyboard);
    
    boolean updateById(Storyboard storyboard);
    
    boolean removeById(Long id);
    
    boolean deleteByScriptId(Long scriptId);
}