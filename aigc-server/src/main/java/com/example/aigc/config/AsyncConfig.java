package com.example.aigc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "videoPipelineExecutor")
    public Executor videoPipelineExecutor(PipelineVideoProperties pipelineVideoProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int maxParallel = Math.max(1, pipelineVideoProperties.getMaxParallel());
        executor.setCorePoolSize(maxParallel);
        executor.setMaxPoolSize(maxParallel);
        executor.setQueueCapacity(Math.max(16, maxParallel * 8));
        executor.setThreadNamePrefix("video-pipeline-");
        executor.initialize();
        return executor;
    }
}
