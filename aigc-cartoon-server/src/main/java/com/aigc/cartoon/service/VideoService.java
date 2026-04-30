package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.Video;
import java.util.List;

public interface VideoService {
    
    List<Video> listByProjectId(Long projectId);
    
    Video getById(Long id);
    
    boolean save(Video video);
    
    boolean updateById(Video video);
    
    boolean removeById(Long id);
    
    boolean deleteByProjectId(Long projectId);
}