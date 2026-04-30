package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础Controller - 提供健康检查和欢迎信息
 */
@RestController
@RequestMapping("/api")
public class HelloController {
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AIGC漫剧工具后端服务运行正常");
    }
    
    /**
     * 欢迎接口
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("欢迎使用AIGC漫剧工具");
    }
}
