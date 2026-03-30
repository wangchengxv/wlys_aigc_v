package com.example.aigc.config;

import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.FileConnectionConfigRepository;
import com.example.aigc.repository.FileGenerationTaskRepository;
import com.example.aigc.repository.FileModelConfigRepository;
import com.example.aigc.repository.FileRouterApiKeyRepository;
import com.example.aigc.repository.FileRouterRequestLogRepository;
import com.example.aigc.repository.FileRoutingConfigRepository;
import com.example.aigc.repository.FileScriptProjectRepository;
import com.example.aigc.repository.InMemoryConnectionConfigRepository;
import com.example.aigc.repository.InMemoryModelConfigRepository;
import com.example.aigc.repository.GenerationTaskRepository;
import com.example.aigc.repository.JsonFileStorageSupport;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.repository.RouterApiKeyRepository;
import com.example.aigc.repository.RouterRequestLogRepository;
import com.example.aigc.repository.RoutingConfigRepository;
import com.example.aigc.repository.ScriptProjectRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    public ConnectionConfigRepository connectionConfigRepository(JsonFileStorageSupport storageSupport) {
        return new FileConnectionConfigRepository(storageSupport);
    }

    @Bean
    public ModelConfigRepository modelConfigRepository(JsonFileStorageSupport storageSupport) {
        return new FileModelConfigRepository(storageSupport);
    }

    @Bean
    public GenerationTaskRepository generationTaskRepository(JsonFileStorageSupport storageSupport) {
        return new FileGenerationTaskRepository(storageSupport);
    }

    @Bean
    public RouterApiKeyRepository routerApiKeyRepository(JsonFileStorageSupport storageSupport) {
        return new FileRouterApiKeyRepository(storageSupport);
    }

    @Bean
    public RoutingConfigRepository routingConfigRepository(JsonFileStorageSupport storageSupport) {
        return new FileRoutingConfigRepository(storageSupport);
    }

    @Bean
    public RouterRequestLogRepository routerRequestLogRepository(JsonFileStorageSupport storageSupport) {
        return new FileRouterRequestLogRepository(storageSupport);
    }

    @Bean
    public ScriptProjectRepository scriptProjectRepository(JsonFileStorageSupport storageSupport) {
        return new FileScriptProjectRepository(storageSupport);
    }
}
