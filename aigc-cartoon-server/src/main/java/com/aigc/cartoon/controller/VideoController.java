package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import com.aigc.cartoon.entity.Video;
import com.aigc.cartoon.service.VideoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoController {
    
    private final VideoService videoService;
    
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }
    
    @GetMapping("/projects/{projectId}/videos")
    public Result<List<Video>> listByProject(@PathVariable Long projectId) {
        List<Video> videos = videoService.listByProjectId(projectId);
        return Result.success(videos);
    }
    
    @GetMapping("/videos/{id}")
    public Result<Video> getById(@PathVariable Long id) {
        Video video = videoService.getById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        return Result.success(video);
    }
    
    @PostMapping("/videos")
    public Result<Video> create(@RequestBody Video video) {
        videoService.save(video);
        return Result.success(video);
    }
    
    @PutMapping("/videos/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Video video) {
        video.setId(id);
        boolean success = videoService.updateById(video);
        return Result.success(success);
    }
    
    @DeleteMapping("/videos/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = videoService.removeById(id);
        return Result.success(success);
    }
}