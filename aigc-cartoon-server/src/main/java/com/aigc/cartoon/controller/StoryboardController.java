package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import com.aigc.cartoon.entity.Storyboard;
import com.aigc.cartoon.service.StoryboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StoryboardController {
    
    private final StoryboardService storyboardService;
    
    public StoryboardController(StoryboardService storyboardService) {
        this.storyboardService = storyboardService;
    }
    
    @GetMapping("/scripts/{scriptId}/storyboards")
    public Result<List<Storyboard>> listByScript(@PathVariable Long scriptId) {
        List<Storyboard> storyboards = storyboardService.listByScriptId(scriptId);
        return Result.success(storyboards);
    }
    
    @GetMapping("/storyboards/{id}")
    public Result<Storyboard> getById(@PathVariable Long id) {
        Storyboard storyboard = storyboardService.getById(id);
        if (storyboard == null) {
            return Result.error("分镜不存在");
        }
        return Result.success(storyboard);
    }
    
    @PostMapping("/storyboards")
    public Result<Storyboard> create(@RequestBody Storyboard storyboard) {
        storyboardService.save(storyboard);
        return Result.success(storyboard);
    }
    
    @PutMapping("/storyboards/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Storyboard storyboard) {
        storyboard.setId(id);
        boolean success = storyboardService.updateById(storyboard);
        return Result.success(success);
    }
    
    @DeleteMapping("/storyboards/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = storyboardService.removeById(id);
        return Result.success(success);
    }
    
    @PostMapping("/storyboards/batch")
    public Result<Boolean> batchCreate(@RequestBody List<Storyboard> storyboards) {
        if (storyboards == null || storyboards.isEmpty()) {
            return Result.badRequest("分镜数据不能为空");
        }
        for (Storyboard storyboard : storyboards) {
            storyboardService.save(storyboard);
        }
        return Result.success(true);
    }
}