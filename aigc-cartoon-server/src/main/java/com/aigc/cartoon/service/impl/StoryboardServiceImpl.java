package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.Storyboard;
import com.aigc.cartoon.mapper.StoryboardMapper;
import com.aigc.cartoon.service.StoryboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryboardServiceImpl extends ServiceImpl<StoryboardMapper, Storyboard> implements StoryboardService {
    
    @Override
    public List<Storyboard> listByScriptId(Long scriptId) {
        LambdaQueryWrapper<Storyboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Storyboard::getScriptId, scriptId);
        wrapper.orderByAsc(Storyboard::getSceneNumber);
        return list(wrapper);
    }
    
    @Override
    public Storyboard getById(Long id) {
        return baseMapper.selectById(id);
    }
    
    @Override
    public boolean save(Storyboard storyboard) {
        return baseMapper.insert(storyboard) > 0;
    }
    
    @Override
    public boolean updateById(Storyboard storyboard) {
        return baseMapper.updateById(storyboard) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
    
    @Override
    public boolean deleteByScriptId(Long scriptId) {
        LambdaQueryWrapper<Storyboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Storyboard::getScriptId, scriptId);
        return baseMapper.delete(wrapper) >= 0;
    }
}