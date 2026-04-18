package com.example.aigc.model;

/**
 * Constants for per-function model override keys stored in
 * {@code script_project.workflow_model_overrides}.
 *
 * Each constant identifies one logical step in the script production workflow.
 * The value is stored as-is in the JSON map, so it must be kept stable.
 *
 * Fallback hierarchy (first non-blank wins):
 *   workflowModelOverrides[key]
 *   → project.explicitTextModel / explicitImageModel / explicitVideoModel  (by capability)
 *   → AiCapabilityRoutingService router default
 */
public final class WorkflowModelKey {

    private WorkflowModelKey() {}

    // ── Preview-page text functions ────────────────────────────────────────
    /** 剧本完善 (refine script) */
    public static final String SCRIPT_REFINE = "script_refine";
    /** AI 续写剧本 (append / continue script) */
    public static final String SCRIPT_APPEND = "script_append";
    /** AI 剧本改写 (rewrite script) */
    public static final String SCRIPT_REWRITE = "script_rewrite";
    /** 三阶段优化 - 场次 (optimize scenes) */
    public static final String OPTIMIZE_SCENES = "optimize_scenes";
    /** 三阶段优化 - 角色 (optimize characters) */
    public static final String OPTIMIZE_CHARACTERS = "optimize_characters";
    /** 三阶段优化 - 道具 (optimize props) */
    public static final String OPTIMIZE_PROPS = "optimize_props";

    // ── Assets-page image functions ────────────────────────────────────────
    /** B-1 美术指导 (art direction) */
    public static final String ART_DIRECTION = "art_direction";
    /** B-2 角色视觉提示词 (character visual prompt) */
    public static final String CHARACTER_VISUAL_PROMPT = "character_visual_prompt";
    /** 生成关键帧 / 重生成关键帧 (keyframe generation) */
    public static final String KEYFRAME_IMAGE = "keyframe_image";
    /** B-6 九宫格规划 (turnaround plan) */
    public static final String TURNAROUND_PLAN = "turnaround_plan";
    /** B-7 九宫格造型图 (turnaround image) */
    public static final String TURNAROUND_IMAGE = "turnaround_image";
    /** 分镜九宫格规划 (storyboard plan) */
    public static final String STORYBOARD_PLAN = "storyboard_plan";
    /** 分镜九宫格出图 (storyboard image) */
    public static final String STORYBOARD_IMAGE = "storyboard_image";
    /** B-8 群像概念图 (group scene image) */
    public static final String GROUP_SCENE_IMAGE = "group_scene_image";
    /** 三视图 (three-view image) */
    public static final String THREE_VIEW_IMAGE = "three_view_image";

    // ── Video-page functions ───────────────────────────────────────────────
    /** B-9 分镜图像提示词 (shot visual prompt) */
    public static final String SHOT_VISUAL_PROMPT = "shot_visual_prompt";
    /** 视频生成 (video generation) */
    public static final String VIDEO_GENERATION = "video_generation";
    /** 配音 / TTS 生成 (tts dubbing generation) */
    public static final String TTS_DUBBING = "tts_dubbing";
    /** 口型同步 (lip sync generation) */
    public static final String LIP_SYNC = "lip_sync";
}
