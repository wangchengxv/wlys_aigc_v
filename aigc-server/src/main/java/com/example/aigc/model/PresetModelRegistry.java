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
        new PresetModel("onelinkai", "kling-v2-1", "https://api.onelinkai.cloud", "OneLinkAI Kling 图像生成 v2.1", List.of("image")),
        new PresetModel("onelinkai", "kling-v2", "https://api.onelinkai.cloud", "OneLinkAI Kling 多图参考生图 v2", List.of("image")),
        new PresetModel("onelinkai", "MiniMax-M2.1", "https://api.onelinkai.cloud", "OneLinkAI MiniMax M2.1", List.of("video")),
        new PresetModel("onelinkai", "kling-v2-6", "https://api.onelinkai.cloud", "OneLinkAI Kling 文生视频 v2.6", List.of("video")),
        new PresetModel("onelinkai", "kling-v1", "https://api.onelinkai.cloud", "OneLinkAI Kling 图生视频 v1", List.of("video")),
        new PresetModel("onelinkai", "viduq3-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Turbo", List.of("video")),
        new PresetModel("onelinkai", "viduq3-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Pro", List.of("video")),
        new PresetModel("onelinkai", "viduq2-pro-fast", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro Fast", List.of("video")),
        new PresetModel("onelinkai", "viduq2-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro", List.of("video")),
        new PresetModel("onelinkai", "viduq2-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Turbo", List.of("video")),
        new PresetModel("onelinkai", "viduq2", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2", List.of("video")),
        new PresetModel("onelinkai", "viduq1", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1", List.of("video")),
        new PresetModel("onelinkai", "viduq1-classic", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1 Classic", List.of("video")),
        new PresetModel("onelinkai", "vidu2.0", "https://api.onelinkai.cloud", "OneLinkAI Vidu 2.0", List.of("video")),
        new PresetModel("deepseek", "deepseek-chat", "https://api.deepseek.com", "DeepSeek Chat", List.of("text")),
        new PresetModel("deepseek", "deepseek-reasoner", "https://api.deepseek.com", "DeepSeek Reasoner", List.of("text")),
        new PresetModel("anthropic", "claude-sonnet-4-6", "https://api.anthropic.com", "Claude Sonnet 4.6", List.of("text")),
        new PresetModel("anthropic", "claude-3-5-sonnet-20241022", "https://api.anthropic.com", "Claude 3.5 Sonnet", List.of("text")),
        new PresetModel("anthropic", "claude-3-5-haiku-20241022", "https://api.anthropic.com", "Claude 3.5 Haiku", List.of("text")),
        new PresetModel("qwen", "qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Plus", List.of("text")),
        new PresetModel("qwen", "qwen-turbo", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo", List.of("text")),
        new PresetModel("qwen", "qwen-max", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Max", List.of("text")),
        new PresetModel("aliyun_coding", "qwen-coder-plus", "https://dashscope.aliyuncs.com/compatible-mode", "阿里 Coding Plus", List.of("text")),
        new PresetModel("glm", "glm-4", "https://open.bigmodel.cn/api/paas/v4", "GLM-4", List.of("text")),
        new PresetModel("glm", "glm-4-flash", "https://open.bigmodel.cn/api/paas/v4", "GLM-4 Flash", List.of("text")),
        new PresetModel("kimi", "moonshot-v1-32k", "https://api.moonshot.cn", "KIMI 32K", List.of("text")),
        new PresetModel("minimax", "abab6.5s-chat", "https://api.minimax.chat", "MiniMax Chat", List.of("text")),
        new PresetModel("ollama", "llama3.1", "http://localhost:11434", "Ollama Llama3.1", List.of("text")),
        new PresetModel("ark", "doubao-seed-2-0-pro-260215", "https://ark.cn-beijing.volces.com", "豆包文本 Seed 2.0 Pro", List.of("text")),
        new PresetModel("ark", "doubao-seedream-5-0-260128", "https://ark.cn-beijing.volces.com", "豆包图片 Seedream", List.of("image")),
        new PresetModel("ark", "doubao-seedance-2-0-260128", "https://ark.cn-beijing.volces.com", "豆包视频 Seedance 2.0（标准版）", List.of("video")),
        new PresetModel("ark", "doubao-seedance-2-0-fast-260128", "https://ark.cn-beijing.volces.com", "豆包视频 Seedance 2.0（极速版）", List.of("video")),
        new PresetModel("moark", "Wan2.1-I2V-14B-720P", "https://api.moark.com", "Moark Wan2.1 图生视频 720P", List.of("video")),
        new PresetModel("vidu", "viduq3-turbo", "https://api.vidu.cn", "Vidu Q3 Turbo", List.of("video")),
        new PresetModel("vidu", "viduq3-pro", "https://api.vidu.cn", "Vidu Q3 Pro", List.of("video")),
        new PresetModel("vidu", "viduq2-pro-fast", "https://api.vidu.cn", "Vidu Q2 Pro Fast", List.of("video")),
        new PresetModel("vidu", "viduq2-pro", "https://api.vidu.cn", "Vidu Q2 Pro", List.of("video")),
        new PresetModel("vidu", "viduq2-turbo", "https://api.vidu.cn", "Vidu Q2 Turbo", List.of("video")),
        new PresetModel("vidu", "viduq2", "https://api.vidu.cn", "Vidu Q2", List.of("video")),
        new PresetModel("vidu", "viduq1", "https://api.vidu.cn", "Vidu Q1", List.of("video")),
        new PresetModel("vidu", "viduq1-classic", "https://api.vidu.cn", "Vidu Q1 Classic", List.of("video")),
        new PresetModel("vidu", "vidu2.0", "https://api.vidu.cn", "Vidu 2.0", List.of("video")),
        new PresetModel("vidu_onelink", "viduq3-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Turbo", List.of("video")),
        new PresetModel("vidu_onelink", "viduq3-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Pro", List.of("video")),
        new PresetModel("vidu_onelink", "viduq2-pro-fast", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro Fast", List.of("video")),
        new PresetModel("vidu_onelink", "viduq2-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro", List.of("video")),
        new PresetModel("vidu_onelink", "viduq2-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Turbo", List.of("video")),
        new PresetModel("vidu_onelink", "viduq2", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2", List.of("video")),
        new PresetModel("vidu_onelink", "viduq1", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1", List.of("video")),
        new PresetModel("vidu_onelink", "viduq1-classic", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1 Classic", List.of("video")),
        new PresetModel("vidu_onelink", "vidu2.0", "https://api.onelinkai.cloud", "OneLinkAI Vidu 2.0", List.of("video")),
        new PresetModel("kling", "kling-v2-6", "https://api.onelinkai.cloud", "OneLinkAI Kling 文生视频 v2.6", List.of("video")),
        new PresetModel("kling", "kling-v2-1", "https://api.onelinkai.cloud", "OneLinkAI Kling 文生视频 v2.1", List.of("video")),
        new PresetModel("kling", "kling-v1-6", "https://api.onelinkai.cloud", "OneLinkAI Kling 图生视频 v1.6", List.of("video")),
        new PresetModel("kling", "kling-v1", "https://api.onelinkai.cloud", "OneLinkAI Kling 图生视频 v1", List.of("video"))
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
