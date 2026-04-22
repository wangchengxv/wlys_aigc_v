package service

import "strings"

// PresetModel matches Java PresetModelRegistry entries.
type PresetModel struct {
	Provider    string
	ModelName   string
	BaseURL     string
	DisplayName string
	Caps        []string
}

// PresetModels mirrors com.example.aigc.model.PresetModelRegistry.
var PresetModels = []PresetModel{
	{"openai", "gpt-4o", "https://api.openai.com", "GPT-4o", []string{"text"}},
	{"openai", "gpt-4o-mini", "https://api.openai.com", "GPT-4o Mini", []string{"text"}},
	{"openai", "gpt-4-turbo", "https://api.openai.com", "GPT-4 Turbo", []string{"text"}},
	{"openai", "gpt-3.5-turbo", "https://api.openai.com", "GPT-3.5 Turbo", []string{"text"}},
	{"onelinkai", "gpt-4o", "https://api.onelinkai.cloud", "OneLinkAI GPT-4o", []string{"text"}},
	{"onelinkai", "claude-sonnet-4-6", "https://api.onelinkai.cloud", "OneLinkAI Claude Sonnet 4.6", []string{"text"}},
	{"onelinkai", "gemini-2.5-pro", "https://api.onelinkai.cloud", "OneLinkAI Gemini 2.5 Pro", []string{"text"}},
	{"onelinkai", "wanx-v1", "https://api.onelinkai.cloud", "OneLinkAI Wanx v1", []string{"image"}},
	{"onelinkai", "MiniMax-M2.1", "https://api.onelinkai.cloud", "OneLinkAI MiniMax M2.1", []string{"video"}},
	{"vidu", "viduq3-turbo", "https://api.vidu.cn", "Vidu Q3 Turbo", []string{"video"}},
	{"vidu", "viduq3-pro", "https://api.vidu.cn", "Vidu Q3 Pro", []string{"video"}},
	{"vidu", "viduq2-pro-fast", "https://api.vidu.cn", "Vidu Q2 Pro Fast", []string{"video"}},
	{"vidu", "viduq2-pro", "https://api.vidu.cn", "Vidu Q2 Pro", []string{"video"}},
	{"vidu", "viduq2-turbo", "https://api.vidu.cn", "Vidu Q2 Turbo", []string{"video"}},
	{"vidu", "viduq2", "https://api.vidu.cn", "Vidu Q2", []string{"video"}},
	{"vidu", "viduq1", "https://api.vidu.cn", "Vidu Q1", []string{"video"}},
	{"vidu", "viduq1-classic", "https://api.vidu.cn", "Vidu Q1 Classic", []string{"video"}},
	{"vidu", "vidu2.0", "https://api.vidu.cn", "Vidu 2.0", []string{"video"}},
	{"vidu_onelink", "viduq3-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Turbo", []string{"video"}},
	{"vidu_onelink", "viduq3-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Pro", []string{"video"}},
	{"vidu_onelink", "viduq2-pro-fast", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro Fast", []string{"video"}},
	{"vidu_onelink", "viduq2-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro", []string{"video"}},
	{"vidu_onelink", "viduq2-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Turbo", []string{"video"}},
	{"vidu_onelink", "viduq2", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2", []string{"video"}},
	{"vidu_onelink", "viduq1", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1", []string{"video"}},
	{"vidu_onelink", "viduq1-classic", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1 Classic", []string{"video"}},
	{"vidu_onelink", "vidu2.0", "https://api.onelinkai.cloud", "OneLinkAI Vidu 2.0", []string{"video"}},
	{"kling", "kling-v2-6", "https://api.onelinkai.cloud", "OneLinkAI Kling 文生视频 v2.6", []string{"video"}},
	{"kling", "kling-v2-1", "https://api.onelinkai.cloud", "OneLinkAI Kling 文生视频 v2.1", []string{"video"}},
	{"kling", "kling-v1-6", "https://api.onelinkai.cloud", "OneLinkAI Kling 图生视频 v1.6", []string{"video"}},
	{"kling", "kling-v1", "https://api.onelinkai.cloud", "OneLinkAI Kling 图生视频 v1", []string{"video"}},
	{"deepseek", "deepseek-chat", "https://api.deepseek.com", "DeepSeek Chat", []string{"text"}},
	{"deepseek", "deepseek-reasoner", "https://api.deepseek.com", "DeepSeek Reasoner", []string{"text"}},
	{"anthropic", "claude-sonnet-4-6", "https://api.anthropic.com", "Claude Sonnet 4.6", []string{"text"}},
	{"anthropic", "claude-3-5-sonnet-20241022", "https://api.anthropic.com", "Claude 3.5 Sonnet", []string{"text"}},
	{"anthropic", "claude-3-5-haiku-20241022", "https://api.anthropic.com", "Claude 3.5 Haiku", []string{"text"}},
	{"qwen", "qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Plus", []string{"text"}},
	{"qwen", "qwen-turbo", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo", []string{"text"}},
	{"qwen", "qwen-max", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Max", []string{"text"}},
	{"aliyun_coding", "qwen-coder-plus", "https://dashscope.aliyuncs.com/compatible-mode", "阿里 Coding Plus", []string{"text"}},
	{"glm", "glm-4", "https://open.bigmodel.cn/api/paas/v4", "GLM-4", []string{"text"}},
	{"glm", "glm-4-flash", "https://open.bigmodel.cn/api/paas/v4", "GLM-4 Flash", []string{"text"}},
	{"kimi", "moonshot-v1-32k", "https://api.moonshot.cn", "KIMI 32K", []string{"text"}},
	{"minimax", "abab6.5s-chat", "https://api.minimax.chat", "MiniMax Chat", []string{"text"}},
	{"ollama", "llama3.1", "http://localhost:11434", "Ollama Llama3.1", []string{"text"}},
	{"ark", "doubao-seed-2-0-pro-260215", "https://ark.cn-beijing.volces.com", "豆包文本 Seed 2.0 Pro", []string{"text"}},
	{"ark", "doubao-seedream-5-0-260128", "https://ark.cn-beijing.volces.com", "豆包图片 Seedream", []string{"image"}},
	{"ark", "doubao-seedance-2-0-260128", "https://ark.cn-beijing.volces.com", "豆包视频 Seedance 2.0（标准版）", []string{"video"}},
	{"ark", "doubao-seedance-2-0-fast-260128", "https://ark.cn-beijing.volces.com", "豆包视频 Seedance 2.0（极速版）", []string{"video"}},
	{"moark", "Wan2.1-I2V-14B-720P", "https://api.moark.com", "Moark Wan2.1 图生视频 720P", []string{"video"}},
}

// FindPreset returns a preset by provider + model name (case-insensitive).
func FindPreset(provider, modelName string) *PresetModel {
	p := normalizeProv(provider)
	for i := range PresetModels {
		if normalizeProv(PresetModels[i].Provider) != p {
			continue
		}
		if strings.EqualFold(strings.TrimSpace(PresetModels[i].ModelName), strings.TrimSpace(modelName)) {
			return &PresetModels[i]
		}
	}
	return nil
}

func normalizeProv(s string) string {
	return strings.ToLower(strings.TrimSpace(s))
}
