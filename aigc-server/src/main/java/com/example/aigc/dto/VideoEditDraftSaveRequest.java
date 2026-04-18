package com.example.aigc.dto;

import com.example.aigc.entity.VideoEditSegment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VideoEditDraftSaveRequest {
    public Integer expectedVersion;
    public List<VideoEditSegment> segments = new ArrayList<>();
    public Map<String, Object> extensions = new LinkedHashMap<>();
}
