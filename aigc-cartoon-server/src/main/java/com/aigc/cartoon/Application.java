package com.aigc.cartoon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AIGC漫剧工具后端服务主启动类
 */
@SpringBootApplication
@MapperScan("com.aigc.cartoon.mapper")
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
