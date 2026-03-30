package com.example.aigc.entity;

import com.example.aigc.enums.SegmentTaskStatus;

import java.time.Instant;

public class VideoSegmentTask {
    public String segmentTaskId;
    public String projectId;
    public String shotId;
    public String requestPayloadFileId;
    public String resultVideoFileId;
    public String providerTaskId;
    public SegmentTaskStatus status = SegmentTaskStatus.PENDING;
    public Integer retryCount = 0;
    public String modelName;
    public Instant startedAt;
    public Instant finishedAt;
    public String errorMessage;
}
