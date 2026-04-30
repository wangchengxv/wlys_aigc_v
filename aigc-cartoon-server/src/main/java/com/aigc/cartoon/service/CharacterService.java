package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.Character;
import java.util.List;

public interface CharacterService {
    
    List<Character> listByProjectId(Long projectId);
    
    Character getById(Long id);
    
    boolean save(Character character);
    
    boolean updateById(Character character);
    
    boolean removeById(Long id);
    
    boolean deleteByProjectId(Long projectId);
}