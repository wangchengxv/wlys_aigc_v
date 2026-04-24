package com.example.aigc.model;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PresetModelRegistry {

    private final List<PresetModel> models = List.of(
        new PresetModel("openai", "gpt-4o", "https://api.openai.com", "GPT-4o", List.of("text")),
        new PresetModel("openai", "gpt-4o-mini", "https://api.openai.com", "GPT-4o Mini", List.of("text")),
        new PresetModel("openai", "gpt-4-turbo", "https://api.openai.com", "GPT-4 Turbo", List.of("text")),
        new PresetModel("openai", "gpt-3.5-turbo", "https://api.openai.com", "GPT-3.5 Turbo", List.of("text")),
        new PresetModel("onelinkai", "gpt-4o", "https://api.onelinkai.cloud", "OneLinkAI GPT-4o", List.of("text")),
        new PresetModel("onelinkai", "claude-sonnet-4-6", "https://api.onelinkai.cloud", "OneLinkAI Claude Sonnet 4.6", List.of("text")),
        new PresetModel("onelinkai", "gemini-2.5-pro", "https://api.onelinkai.cloud", "OneLinkAI Gemini 2.5 Pro", List.of("text")),
        new PresetModel("onelinkai", "wanx-v1", "https://api.onelinkai.cloud", "OneLinkAI Wanx v1", List.of("image")),
        new PresetModel("onelinkai", "doubao-seedance-1.5-pro", "https://api.onelinkai.cloud", "OneLinkAI 豆包 Seedance 1.5 Pro", List.of("video")),
        new PresetModel("onelinkai", "doubao-seedance-2.0", "https://api.onelinkai.cloud", "OneLinkAI 豆包 Seedance 2.0", List.of("video")),
        new PresetModel("onelinkai", "video-kling-v3", "https://api.onelinkai.cloud", "OneLinkAI Kling 图像生成 v2.1", List.of("image")),
        new PresetModel("onelinkai", "video-kling-v3", "https://api.onelinkai.cloud", "OneLinkAI Kling 多图参考生图 v2", List.of("image")),
        new PresetModel("kling", "video-kling-v3-6", "https://api.klingai.com", "可灵官网 Kling v2.6（文生视频）", List.of("video")),
        new PresetModel("kling", "video-kling-v3", "https://api.klingai.com", "可灵官网 Kling v2.1（文生视频）", List.of("video")),
        new PresetModel("kling", "kling-v1-6", "https://api.klingai.com", "可灵官网 Kling v1.6（图生视频）", List.of("video")),
        new PresetModel("kling", "kling-v1", "https://api.klingai.com", "可灵官网 Kling v1（图生视频）", List.of("video")),
        new PresetModel("deepseek", "deepseek-chat", "https://api.deepseek.com", "DeepSeek Chat", List.of("text")),
        new PresetModel("deepseek", "deepseek-reasoner", "https://api.deepseek.com", "DeepSeek Reasoner", List.of("text")),
        new PresetModel("anthropic", "claude-sonnet-4-6", "https://api.anthropic.com", "Claude Sonnet 4.6", List.of("text")),
        new PresetModel("anthropic", "claude-3-5-sonnet-20241022", "https://api.anthropic.com", "Claude 3.5 Sonnet", List.of("text")),
        new PresetModel("anthropic", "claude-3-5-haiku-20241022", "https://api.anthropic.com", "Claude 3.5 Haiku", List.of("text")),
        new PresetModel("qwen", "qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Plus", List.of("text")),
        new PresetModel("qwen", "qwen-turbo", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo", List.of("text")),
        new PresetModel("qwen", "qwen-turbo-latest", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo 最新版", List.of("text")),
        new PresetModel("qwen", "qwen-max", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Max", List.of("text")),
        new PresetModel("aliyun_coding", "qwen-coder-plus", "https://dashscope.aliyuncs.com/compatible-mode", "阿里 Coding Plus", List.of("text")),
        new PresetModel("glm", "glm-4", "https://open.bigmodel.cn/api/paas/v4", "GLM-4", List.of("text")),
        new PresetModel("glm", "glm-4-flash", "https://open.bigmodel.cn/api/paas/v4", "GLM-4 Flash", List.of("text")),
        new PresetModel("kimi", "moonshot-v1-32k", "https://api.moonshot.cn", "KIMI 32K", List.of("text")),
        new PresetModel("minimax", "abab6.5s-chat", "https://api.minimax.chat", "MiniMax Chat", List.of("text")),
        new PresetModel("ollama", "llama3.1", "http://localhost:11434", "Ollama Llama3.1", List.of("text")),
        new PresetModel("ark", "doubao-seed-2-0-pro-260215", "https://ark.cn-beijing.volces.com", "豆包文本 Seed 2.0 Pro", List.of("text")),
        new PresetModel("ark", "doubao-seedream-5-0-260128", "https://ark.cn-beijing.volces.com", "豆包图片 Seedream", List.of("image"))
    );

    public List<PresetModel> getAll() {
        return models;
    }

    public List<PresetModel> getByProvider(String provider) {
        return models.stream()
                .filter(m -> m.getProvider().equalsIgnoreCase(provider))
                .toList();
    }

    public PresetModel find(String provider, String modelName) {
        return models.stream()
                .filter(m -> m.getProvider().equalsIgnoreCase(provider)
                        && m.getModelName().equalsIgnoreCase(modelName))
                .findFirst()
                .orElse(null);
    }

    public List<String> getProviders() {
        return models.stream()
                .map(PresetModel::getProvider)
                .distinct()
                .toList();
    }
}
