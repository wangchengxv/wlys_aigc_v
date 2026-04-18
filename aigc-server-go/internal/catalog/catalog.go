package catalog

import (
	"strings"

	"github.com/example/aigc-server-go/internal/errs"
)

type AuthMode int

const (
	AuthBearer AuthMode = iota
	AuthXAPIKey
	AuthAPIKeyHeader
	AuthToken
	AuthNone
)

// JavaName matches Java ProviderCatalog.AuthMode.name() for API responses.
func (m AuthMode) JavaName() string {
	switch m {
	case AuthBearer:
		return "BEARER"
	case AuthXAPIKey:
		return "X_API_KEY"
	case AuthAPIKeyHeader:
		return "API_KEY_HEADER"
	case AuthToken:
		return "TOKEN"
	case AuthNone:
		return "NONE"
	default:
		return "NONE"
	}
}

// GatewayKind matches Java GatewayKind
const (
	KindOpenAICompat = "OPENAI_COMPAT"
	KindAnthropic    = "ANTHROPIC"
	KindAzure        = "AZURE_OPENAI"
	KindBedrock      = "BEDROCK"
	KindVertex       = "VERTEX"
	KindOllama       = "OLLAMA"
	KindMoarkI2V     = "MOARK_I2V"
)

// Provider mirrors ProviderDefinition record
type Provider struct {
	Key                    string
	DisplayName            string
	DefaultBaseURL         string
	APIFormat              string
	ChatPath               string
	ModelsPath             string
	AuthMode               AuthMode
	TextProxySupported     bool
	ImageGenerationPath    string
	VideoSubmitPath        string
	VideoResultPath        string
	Kind                   string
	StaticModels           []string
	VideoTaskStatusBaseURL string
	VideoSubmitMultipart   bool
}

type Catalog struct {
	byKey    map[string]*Provider
	aliasMap map[string]string
}

func normalizeAlias(s string) string {
	s = strings.TrimSpace(strings.ToLower(s))
	s = strings.ReplaceAll(s, " ", "")
	s = strings.ReplaceAll(s, "-", "")
	s = strings.ReplaceAll(s, "_", "")
	return s
}

func (c *Catalog) Require(raw string) (*Provider, error) {
	key := c.Normalize(raw)
	p := c.byKey[key]
	if p == nil {
		return nil, errs.New(400, "不支持的提供商: "+raw)
	}
	return p, nil
}

func (c *Catalog) Normalize(raw string) string {
	if strings.TrimSpace(raw) == "" {
		return ""
	}
	if k := c.aliasMap[normalizeAlias(raw)]; k != "" {
		return k
	}
	return strings.ToLower(strings.TrimSpace(raw))
}

func (c *Catalog) List() []*Provider {
	out := make([]*Provider, 0, len(c.byKey))
	for _, p := range c.byKey {
		out = append(out, p)
	}
	return out
}

func New() *Catalog {
	by := map[string]*Provider{}
	aliases := map[string]string{}

	reg := func(p Provider, extraAliases ...string) {
		by[p.Key] = &p
		aliases[normalizeAlias(p.Key)] = p.Key
		aliases[normalizeAlias(p.DisplayName)] = p.Key
		for _, a := range extraAliases {
			aliases[normalizeAlias(a)] = p.Key
		}
	}

	reg(Provider{
		Key: "openai", DisplayName: "OpenAI", DefaultBaseURL: "https://api.openai.com",
		APIFormat: "openai", ChatPath: "/v1/chat/completions", ModelsPath: "/v1/models",
		AuthMode: AuthBearer, TextProxySupported: true, ImageGenerationPath: "/v1/images/generations",
		Kind: KindOpenAICompat,
	}, "gpt", "chatgpt")

	reg(Provider{
		Key: "onelinkai", DisplayName: "OneLinkAI", DefaultBaseURL: "https://api.onelinkai.cloud",
		APIFormat: "openai", ChatPath: "/v1/chat/completions", ModelsPath: "/v1/models",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"gpt-4o", "gpt-4o-mini", "claude-sonnet-4-6", "gemini-2.5-pro", "gemini-2.5-flash",
			"wanx-v1", "MiniMax-M2.1", "viduq3-turbo", "viduq3-pro", "viduq2-pro-fast", "viduq2-pro", "viduq2-turbo",
			"viduq2", "viduq1", "viduq1-classic", "vidu2.0"},
	}, "onelink", "onelink-ai", "一键ai")

	reg(Provider{
		Key: "anthropic", DisplayName: "Anthropic", DefaultBaseURL: "https://api.anthropic.com",
		APIFormat: "anthropic", ChatPath: "/v1/messages",
		AuthMode: AuthXAPIKey, TextProxySupported: true, Kind: KindAnthropic,
		StaticModels: []string{"claude-opus-4-6", "claude-sonnet-4-6", "claude-haiku-4-5-20251001",
			"claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229"},
	}, "claude")

	reg(Provider{
		Key: "deepseek", DisplayName: "DeepSeek", DefaultBaseURL: "https://api.deepseek.com",
		APIFormat: "openai", ChatPath: "/v1/chat/completions",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"deepseek-chat", "deepseek-reasoner", "deepseek-coder"},
	})

	reg(Provider{
		Key: "qwen", DisplayName: "通义千问", DefaultBaseURL: "https://dashscope.aliyuncs.com/compatible-mode",
		APIFormat: "openai", ChatPath: "/v1/chat/completions",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"qwen-max", "qwen-plus", "qwen-turbo", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct"},
	}, "通义千问", "aliyun", "dashscope")

	reg(Provider{
		Key: "minimax", DisplayName: "MiniMax", DefaultBaseURL: "https://api.minimax.chat",
		APIFormat: "openai", ChatPath: "/v1/chat/completions",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"MiniMax-Text-01", "abab6.5s-chat", "abab6.5g-chat", "abab5.5-chat"},
	})

	reg(Provider{
		Key: "glm", DisplayName: "智谱AI", DefaultBaseURL: "https://open.bigmodel.cn/api/paas/v4",
		APIFormat: "openai", ChatPath: "/chat/completions",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"glm-4-plus", "glm-4-air", "glm-4-flash", "glm-4", "glm-3-turbo"},
	}, "zhipu", "智谱", "bigmodel", "智谱ai")

	reg(Provider{
		Key: "kimi", DisplayName: "KIMI", DefaultBaseURL: "https://api.moonshot.cn",
		APIFormat: "openai", ChatPath: "/v1/chat/completions",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"},
	}, "moonshot", "月之暗面")

	reg(Provider{
		Key: "aliyun_coding", DisplayName: "阿里 Coding", DefaultBaseURL: "https://dashscope.aliyuncs.com/compatible-mode",
		APIFormat: "openai", ChatPath: "/v1/chat/completions",
		AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
		StaticModels: []string{"qwen-coder-plus", "qwen-coder-turbo", "qwen2.5-coder-32b-instruct"},
	}, "coding", "aliyun_coding", "阿里coding", "coding plan")

	reg(Provider{
		Key: "ollama", DisplayName: "Ollama", DefaultBaseURL: "http://localhost:11434",
		APIFormat: "openai", ChatPath: "/v1/chat/completions", ModelsPath: "/api/tags",
		AuthMode: AuthNone, TextProxySupported: true, Kind: KindOllama,
	})

	reg(Provider{
		Key: "lm_studio", DisplayName: "LM Studio", DefaultBaseURL: "http://127.0.0.1:1234",
		APIFormat: "openai", ChatPath: "/v1/chat/completions", ModelsPath: "/v1/models",
		AuthMode: AuthNone, TextProxySupported: true, Kind: KindOpenAICompat,
	}, "lmstudio", "lmstudioai")

	reg(Provider{
		Key: "azure_openai", DisplayName: "Azure OpenAI", DefaultBaseURL: "https://YOUR_RESOURCE.openai.azure.com",
		APIFormat: "openai", ChatPath: "/openai/deployments/{deployment}/chat/completions", ModelsPath: "/openai/models",
		AuthMode: AuthAPIKeyHeader, TextProxySupported: true, Kind: KindAzure,
	})

	reg(Provider{
		Key: "aws_bedrock", DisplayName: "AWS Bedrock", DefaultBaseURL: "https://bedrock-runtime.us-east-1.amazonaws.com",
		APIFormat: "openai", AuthMode: AuthNone, TextProxySupported: true, Kind: KindBedrock,
	}, "bedrock", "awsbedrock")

	reg(Provider{
		Key: "vertex_ai", DisplayName: "Vertex AI", DefaultBaseURL: "https://us-central1-aiplatform.googleapis.com",
		APIFormat: "openai", AuthMode: AuthNone, TextProxySupported: true, Kind: KindVertex,
	}, "vertex", "googlevertex")

	reg(Provider{
		Key: "ark", DisplayName: "方舟", DefaultBaseURL: "https://ark.cn-beijing.volces.com",
		APIFormat: "openai", ChatPath: "/api/v3/responses",
		AuthMode: AuthBearer, TextProxySupported: true,
		ImageGenerationPath: "/api/v3/images/generations",
		VideoSubmitPath:     "/api/v3/contents/generations/tasks",
		VideoResultPath:     "/api/v3/contents/generations/tasks/{taskId}",
		Kind:                KindOpenAICompat,
	}, "doubao", "火山方舟", "方舟")

	reg(Provider{
		Key: "moark", DisplayName: "Moark", DefaultBaseURL: "https://api.moark.com",
		APIFormat: "openai", AuthMode: AuthBearer, TextProxySupported: false,
		VideoSubmitPath: "/v1/async/videos/image-to-video", VideoResultPath: "/v1/task/{taskId}",
		Kind: KindMoarkI2V, StaticModels: []string{"Wan2.1-I2V-14B-720P"},
		VideoTaskStatusBaseURL: "https://moark.com", VideoSubmitMultipart: true,
	})

	reg(Provider{
		Key: "vidu", DisplayName: "Vidu", DefaultBaseURL: "https://api.vidu.cn",
		APIFormat: "vidu", AuthMode: AuthToken, TextProxySupported: false,
		VideoSubmitPath: "/ent/v2/img2video", VideoResultPath: "/ent/v2/tasks/{taskId}/creations",
		Kind: KindOpenAICompat,
		StaticModels: []string{
			"viduq3-turbo", "viduq3-pro", "viduq2-pro-fast", "viduq2-pro", "viduq2-turbo",
			"viduq2", "viduq1", "viduq1-classic", "vidu2.0",
		},
	}, "viduai")

	return &Catalog{byKey: by, aliasMap: aliases}
}
