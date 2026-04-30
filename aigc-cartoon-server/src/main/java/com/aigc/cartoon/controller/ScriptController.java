package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import com.aigc.cartoon.entity.Script;
import com.aigc.cartoon.service.ScriptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScriptController {
    
    private final ScriptService scriptService;
    
    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }
    
    @GetMapping("/projects/{projectId}/scripts")
    public Result<List<Script>> listByProject(@PathVariable Long projectId) {
        List<Script> scripts = scriptService.listByProjectId(projectId);
        return Result.success(scripts);
    }
    
    @GetMapping("/scripts/{id}")
    public Result<Script> getById(@PathVariable Long id) {
        Script script = scriptService.getById(id);
        if (script == null) {
            return Result.error("剧本不存在");
        }
        return Result.success(script);
    }
    
    @GetMapping("/projects/{projectId}/scripts/latest")
    public Result<Script> getLatestByProject(@PathVariable Long projectId) {
        Script script = scriptService.getLatestByProjectId(projectId);
        if (script == null) {
            return Result.success(null);
        }
        return Result.success(script);
    }
    
    @PostMapping("/scripts")
    public Result<Script> create(@RequestBody Script script) {
        scriptService.save(script);
        return Result.success(script);
    }
    
    @PutMapping("/scripts/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Script script) {
        script.setId(id);
        boolean success = scriptService.updateById(script);
        return Result.success(success);
    }
    
    @DeleteMapping("/scripts/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = scriptService.removeById(id);
        return Result.success(success);
    }
    
    @PostMapping("/projects/{projectId}/scripts/save-or-update")
    public Result<Script> saveOrUpdate(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "剧本");
        String content = body.getOrDefault("content", "");
        
        Script existing = scriptService.getLatestByProjectId(projectId);
        if (existing != null) {
            existing.setContent(content);
            existing.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 2);
            scriptService.updateById(existing);
            return Result.success(existing);
        } else {
            Script script = new Script();
            script.setProjectId(projectId);
            script.setTitle(title);
            script.setContent(content);
            script.setVersion(1);
            scriptService.save(script);
            return Result.success(script);
        }
    }
}