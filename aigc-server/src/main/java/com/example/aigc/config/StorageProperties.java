package com.example.aigc.config;

import com.example.aigc.enums.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aigc.storage")
public class StorageProperties {

    private String dataDir = System.getProperty("user.home") + "/.aigcmanju";
    private StorageProvider provider = StorageProvider.LOCAL;
    private String localBucket = "aigc-local";

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
    }

    public String getLocalBucket() {
        return localBucket;
    }

    public void setLocalBucket(String localBucket) {
        this.localBucket = localBucket;
    }
}
