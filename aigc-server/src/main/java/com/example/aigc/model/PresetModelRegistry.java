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
        new PresetModel("ark", "doubao-seedance-1-5-pro-251215", "https://ark.cn-beijing.volces.com", "豆包视频 Seedance", List.of("video"))
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
