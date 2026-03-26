package com.example.aigc.config;

import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.InMemoryConnectionConfigRepository;
import com.example.aigc.repository.InMemoryModelConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    public ConnectionConfigRepository connectionConfigRepository() {
        return new InMemoryConnectionConfigRepository();
    }

    @Bean
    public ModelConfigRepository modelConfigRepository() {
        return new InMemoryModelConfigRepository();
    }
}