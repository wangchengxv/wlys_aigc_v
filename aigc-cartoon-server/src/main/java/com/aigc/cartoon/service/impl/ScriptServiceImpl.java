package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.Script;
import com.aigc.cartoon.mapper.ScriptMapper;
import com.aigc.cartoon.service.ScriptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScriptServiceImpl extends ServiceImpl<ScriptMapper, Script> implements ScriptService {
    
    @Override
    public List<Script> listByProjectId(Long projectId) {
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Script::getProjectId, projectId);
        wrapper.orderByDesc(Script::getId);
        return list(wrapper);
    }
    
    @Override
    public Script getById(Long id) {
        return baseMapper.selectById(id);
    }
    
    @Override
    public boolean save(Script script) {
        if (script.getVersion() == null) {
            script.setVersion(1);
        }
        return baseMapper.insert(script) > 0;
    }
    
    @Override
    public boolean updateById(Script script) {
        return baseMapper.updateById(script) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
    
    @Override
    public Script getLatestByProjectId(Long projectId) {
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Script::getProjectId, projectId);
        wrapper.orderByDesc(Script::getId);
        wrapper.last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }
}