package com.example.aigc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aigc.storage")
public class StorageProperties {

    private String dataDir = System.getProperty("user.home") + "/.aigcmanju";

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
