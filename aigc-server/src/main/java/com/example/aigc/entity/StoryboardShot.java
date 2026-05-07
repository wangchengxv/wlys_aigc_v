package com.example.aigc.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StoryboardShot {
    public String shotId;
    public String projectId;
    public String parentShotId;
    public Integer sequenceNo;
    public String title;
    public String scriptText;
    public String actionSummary;
    public String cameraMovement;
    /** 该镜头最终分配到的视频目标时长（秒） */
    public Integer targetDurationSec;
    public List<String> characterRefs = new ArrayList<>();
    public List<String> backgroundRefs = new ArrayList<>();
    public List<String> propRefs = new ArrayList<>();
    public List<String> keyframeRefs = new ArrayList<>();
    public String status;
    public Instant createdAt;
    public Instant updatedAt;
}
