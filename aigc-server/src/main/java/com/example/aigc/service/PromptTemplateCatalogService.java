package com.example.aigc.service;

import com.example.aigc.dto.PromptTemplateCatalogItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 首批可编辑模板清单（classpath），用于管理 UI 与默认正文展示。
 */
@Service
public class PromptTemplateCatalogService {

    private final PromptTemplateService promptTemplateService;

    private static final List<CatalogEntry> ENTRIES = List.of(
            new CatalogEntry("prompts/visual/art-direction-system.md", "美术指导·系统", "visual", "结构化美术指导 JSON 的系统提示"),
            new CatalogEntry("prompts/visual/art-direction-user.md", "美术指导·用户", "visual", "剧本与风格上下文"),
            new CatalogEntry("prompts/visual/batch-character-user.md", "批量角色视觉提示词", "visual", "B-2 批量角色"),
            new CatalogEntry("prompts/visual/single-character-user.md", "单角色视觉提示词", "visual", "B-3 单角色"),
            new CatalogEntry("prompts/visual/single-scene-user.md", "单场景视觉提示词", "visual", "B-4 场景"),
            new CatalogEntry("prompts/visual/prop-user.md", "道具视觉提示词", "visual", "B-5 道具"),
            new CatalogEntry("prompts/visual/storyboard-split-system.md", "九宫格分镜·系统", "visual", "分镜拆分系统"),
            new CatalogEntry("prompts/visual/storyboard-split-user.md", "九宫格分镜·用户", "visual", "分镜拆分用户"),
            new CatalogEntry("prompts/visual/shot-storyboard-user.md", "镜头分镜视觉提示词", "visual", "B-9 镜头提示"),
            new CatalogEntry("prompts/keyframe/character-keyframe.md", "角色关键帧", "keyframe", "关键帧生成"),
            new CatalogEntry("prompts/keyframe/background-keyframe.md", "背景关键帧", "keyframe", "背景关键帧"),
            new CatalogEntry("prompts/keyframe/prop-keyframe.md", "道具关键帧", "keyframe", "道具关键帧"),
            new CatalogEntry("prompts/script/rewrite-system.md", "剧本改写·系统", "script", "改写系统提示"),
            new CatalogEntry("prompts/script/rewrite-user.md", "剧本改写·用户", "script", "改写用户模板"),
            new CatalogEntry("prompts/storyboard/build-video-segment.md", "视频段落提示词", "storyboard", "生成视频请求中的文本提示")
    );

    public PromptTemplateCatalogService(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    public List<PromptTemplateCatalogItem> listCatalog() {
        List<PromptTemplateCatalogItem> out = new ArrayList<>();
        for (CatalogEntry e : ENTRIES) {
            String body = promptTemplateService.load(e.path(), "");
            out.add(new PromptTemplateCatalogItem(e.path(), e.title(), e.category(), e.description(), body));
        }
        return out;
    }

    private record CatalogEntry(String path, String title, String category, String description) {
    }
}
