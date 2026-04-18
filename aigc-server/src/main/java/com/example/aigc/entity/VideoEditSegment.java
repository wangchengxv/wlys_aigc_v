package com.example.aigc.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class VideoEditSegment {
    public String segmentId;
    public String shotId;
    public Integer sequenceNo;
    public Boolean enabled = Boolean.TRUE;
    public String sourceType;
    public String sourceTaskId;
    public String sourceFileId;
    public String sourcePublicUrl;
    public Long sourceDurationMs;
    public Long trimInMs;
    public Long trimOutMs;
    public String transitionMode;
    public String trackKey;
    public Map<String, Object> extensions = new LinkedHashMap<>();
}
