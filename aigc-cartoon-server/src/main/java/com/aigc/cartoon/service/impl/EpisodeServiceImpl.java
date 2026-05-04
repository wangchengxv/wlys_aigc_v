package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.Episode;
import com.aigc.cartoon.mapper.EpisodeMapper;
import com.aigc.cartoon.service.EpisodeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EpisodeServiceImpl implements EpisodeService {
    
    private final EpisodeMapper episodeMapper;
    
    public EpisodeServiceImpl(EpisodeMapper episodeMapper) {
        this.episodeMapper = episodeMapper;
    }
    
    @Override
    public List<Episode> listByProjectId(Long projectId) {
        return episodeMapper.selectList(
            new LambdaQueryWrapper<Episode>()
                .eq(Episode::getProjectId, projectId)
                .orderByAsc(Episode::getEpisodeNumber)
        );
    }
    
    @Override
    public Episode getById(Long id) {
        return episodeMapper.selectById(id);
    }
    
    @Override
    public void save(Episode episode) {
        episodeMapper.insert(episode);
    }
    
    @Override
    public boolean updateById(Episode episode) {
        return episodeMapper.updateById(episode) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return episodeMapper.deleteById(id) > 0;
    }
}
