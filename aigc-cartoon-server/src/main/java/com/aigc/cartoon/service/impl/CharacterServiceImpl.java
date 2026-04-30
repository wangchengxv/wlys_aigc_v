package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.Character;
import com.aigc.cartoon.mapper.CharacterMapper;
import com.aigc.cartoon.service.CharacterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, Character> implements CharacterService {
    
    @Override
    public List<Character> listByProjectId(Long projectId) {
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getProjectId, projectId);
        wrapper.orderByAsc(Character::getId);
        return list(wrapper);
    }
    
    @Override
    public Character getById(Long id) {
        return baseMapper.selectById(id);
    }
    
    @Override
    public boolean save(Character character) {
        return baseMapper.insert(character) > 0;
    }
    
    @Override
    public boolean updateById(Character character) {
        return baseMapper.updateById(character) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
    
    @Override
    public boolean deleteByProjectId(Long projectId) {
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getProjectId, projectId);
        return baseMapper.delete(wrapper) >= 0;
    }
}