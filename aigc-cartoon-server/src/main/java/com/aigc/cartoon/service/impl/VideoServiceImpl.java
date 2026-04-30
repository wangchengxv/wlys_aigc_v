package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.Video;
import com.aigc.cartoon.mapper.VideoMapper;
import com.aigc.cartoon.service.VideoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {
    
    @Override
    public List<Video> listByProjectId(Long projectId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getProjectId, projectId);
        wrapper.orderByDesc(Video::getId);
        return list(wrapper);
    }
    
    @Override
    public Video getById(Long id) {
        return baseMapper.selectById(id);
    }
    
    @Override
    public boolean save(Video video) {
        if (video.getStatus() == null) {
            video.setStatus("pending");
        }
        return baseMapper.insert(video) > 0;
    }
    
    @Override
    public boolean updateById(Video video) {
        return baseMapper.updateById(video) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
    
    @Override
    public boolean deleteByProjectId(Long projectId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getProjectId, projectId);
        return baseMapper.delete(wrapper) >= 0;
    }
}