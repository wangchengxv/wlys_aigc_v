package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import com.aigc.cartoon.entity.Episode;
import com.aigc.cartoon.service.EpisodeService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EpisodeController {
    
    private final EpisodeService episodeService;
    
    public EpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }
    
    @GetMapping("/projects/{projectId}/episodes")
    public Result<List<Episode>> listByProject(@PathVariable Long projectId) {
        List<Episode> episodes = episodeService.listByProjectId(projectId);
        return Result.success(episodes);
    }
    
    @GetMapping("/episodes/{id}")
    public Result<Episode> getById(@PathVariable Long id) {
        Episode episode = episodeService.getById(id);
        if (episode == null) {
            return Result.error("剧集不存在");
        }
        return Result.success(episode);
    }
    
    @PostMapping("/episodes")
    public Result<Episode> create(@RequestBody Episode episode) {
        episodeService.save(episode);
        return Result.success(episode);
    }
    
    @PutMapping("/episodes/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Episode episode) {
        episode.setId(id);
        boolean success = episodeService.updateById(episode);
        return Result.success(success);
    }
    
    @DeleteMapping("/episodes/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = episodeService.removeById(id);
        return Result.success(success);
    }
}
