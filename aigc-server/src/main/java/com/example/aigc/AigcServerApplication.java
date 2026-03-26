package com.example.aigc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AigcServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AigcServerApplication.class, args);
    }
}
