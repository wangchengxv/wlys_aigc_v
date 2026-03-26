package com.example.aigc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "aigc.ark")
public class AigcArkProperties {
    private String baseUrl = "https://ark.cn-beijing.volces.com";
    private String apiKey;
    private String defaultImageModel = "doubao-seedream-5-0-260128";
    private List<String> imageModelOptions = new ArrayList<>();
    private String defaultVideoModel = "doubao-seedance-1-5-pro-251215";
    private List<String> videoModelOptions = new ArrayList<>();
    private String videoApiPath = "/api/v3/contents/generations/tasks";
    private String videoResultApiPath = "/api/v3/contents/generations/tasks/{taskId}";
    private int videoPollMaxAttempts = 40;
    private long videoPollIntervalMs = 3000;
    private String responseFormat = "url";
    private String size = "2K";
    private boolean stream = false;
    private boolean watermark = true;
    private String sequentialImageGeneration = "disabled";
    private Integer videoDurationSeconds = 5;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDefaultImageModel() {
        return defaultImageModel;
    }

    public void setDefaultImageModel(String defaultImageModel) {
        this.defaultImageModel = defaultImageModel;
    }

    public List<String> getImageModelOptions() {
        return imageModelOptions;
    }

    public void setImageModelOptions(List<String> imageModelOptions) {
        this.imageModelOptions = imageModelOptions;
    }

    public String getDefaultVideoModel() {
        return defaultVideoModel;
    }

    public void setDefaultVideoModel(String defaultVideoModel) {
        this.defaultVideoModel = defaultVideoModel;
    }

    public List<String> getVideoModelOptions() {
        return videoModelOptions;
    }

    public void setVideoModelOptions(List<String> videoModelOptions) {
        this.videoModelOptions = videoModelOptions;
    }

    public String getVideoApiPath() {
        return videoApiPath;
    }

    public void setVideoApiPath(String videoApiPath) {
        this.videoApiPath = videoApiPath;
    }

    public String getVideoResultApiPath() {
        return videoResultApiPath;
    }

    public void setVideoResultApiPath(String videoResultApiPath) {
        this.videoResultApiPath = videoResultApiPath;
    }

    public int getVideoPollMaxAttempts() {
        return videoPollMaxAttempts;
    }

    public void setVideoPollMaxAttempts(int videoPollMaxAttempts) {
        this.videoPollMaxAttempts = videoPollMaxAttempts;
    }

    public long getVideoPollIntervalMs() {
        return videoPollIntervalMs;
    }

    public void setVideoPollIntervalMs(long videoPollIntervalMs) {
        this.videoPollIntervalMs = videoPollIntervalMs;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean isWatermark() {
        return watermark;
    }

    public void setWatermark(boolean watermark) {
        this.watermark = watermark;
    }

    public String getSequentialImageGeneration() {
        return sequentialImageGeneration;
    }

    public void setSequentialImageGeneration(String sequentialImageGeneration) {
        this.sequentialImageGeneration = sequentialImageGeneration;
    }

    public Integer getVideoDurationSeconds() {
        return videoDurationSeconds;
    }

    public void setVideoDurationSeconds(Integer videoDurationSeconds) {
        this.videoDurationSeconds = videoDurationSeconds;
    }
}
