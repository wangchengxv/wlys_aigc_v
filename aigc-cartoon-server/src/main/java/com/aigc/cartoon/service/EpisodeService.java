package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.Episode;
import java.util.List;

public interface EpisodeService {
    List<Episode> listByProjectId(Long projectId);
    Episode getById(Long id);
    void save(Episode episode);
    boolean updateById(Episode episode);
    boolean removeById(Long id);
}
