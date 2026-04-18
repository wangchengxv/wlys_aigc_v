package com.example.aigc.dto;

import com.example.aigc.entity.VideoEditSegment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VideoEditDraftResponse {
    public String projectId;
    public String draftId;
    public Integer version;
    public Integer publishedVersion;
    public Instant publishedAt;
    public String publishedRenderTaskId;
    public String latestPreviewTaskId;
    public String latestPublishTaskId;
    public boolean hasPublishedResult;
    public boolean hasUnpublishedChanges;
    public List<VideoEditSegment> segments = new ArrayList<>();
    public Map<String, Object> extensions = new LinkedHashMap<>();
}
