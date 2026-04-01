package com.example.aigc.entity;

import com.example.aigc.repository.jpa.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "storyboard_shot")
public class StoryboardShot {
    @Id
    @Column(name = "shot_id")
    public String shotId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "parent_shot_id")
    public String parentShotId;
    @Column(name = "sequence_no")
    public Integer sequenceNo;
    public String title;
    @Column(name = "script_text", columnDefinition = "LONGTEXT")
    public String scriptText;
    @Column(name = "action_summary", columnDefinition = "TEXT")
    public String actionSummary;
    @Column(name = "camera_movement")
    public String cameraMovement;
    /** B-9：镜头类型（结构化，可选） */
    @Column(name = "shot_type")
    public String shotType;
    /** B-9：运镜（结构化，可选） */
    @Column(name = "camera_move")
    public String cameraMove;
    /** B-9：情绪（结构化，可选） */
    @Column(name = "emotion")
    public String emotion;
    /** 该镜头最终分配到的视频目标时长（秒） */
    @Column(name = "target_duration_sec")
    public Integer targetDurationSec;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "character_refs_json", columnDefinition = "LONGTEXT")
    public List<String> characterRefs = new ArrayList<>();
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "background_refs_json", columnDefinition = "LONGTEXT")
    public List<String> backgroundRefs = new ArrayList<>();
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "prop_refs_json", columnDefinition = "LONGTEXT")
    public List<String> propRefs = new ArrayList<>();
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "keyframe_refs_json", columnDefinition = "LONGTEXT")
    public List<String> keyframeRefs = new ArrayList<>();
    @Column(name = "storyboard_asset_id")
    public String storyboardAssetId;
    @Column(name = "storyboard_image_file_id")
    public String storyboardImageFileId;
    @Column(name = "storyboard_crop_file_id")
    public String storyboardCropFileId;
    @Column(name = "storyboard_crop_index")
    public Integer storyboardCropIndex;
    @Column(name = "first_frame_mode")
    public String firstFrameMode;
    @Column(name = "visual_prompt", columnDefinition = "TEXT")
    public String visualPrompt;
    public String status;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
