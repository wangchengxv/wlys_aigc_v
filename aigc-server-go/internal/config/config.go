package config

import (
	"os"
	"strconv"
	"strings"
)

// Config aligns with aigc-server application.yml + env overrides.
type Config struct {
	ServerPort int

	DBHost     string
	DBPort     int
	DBName     string
	DBUser     string
	DBPassword string

	CORSAllowedOriginPatterns []string

	DataDir string

	AuthAccessToken    string
	AuthUserIDRequired bool

	EncryptionKey string

	PipelineVideoMaxParallel    int
	PipelineVideoPollIntervalMs int64
	PipelineVideoMaxRetries     int

	OnelinkAIEnabled                 bool
	OnelinkAIBaseURL                 string
	OnelinkAIModelListRemote         bool
	OnelinkAIModelListTimeoutMs      int
	OnelinkAIModelListFallbackStatic bool

	ComfyBaseURL string

	ArkBaseURL              string
	ArkAPIKey               string
	ArkDefaultImageModel    string
	ArkDefaultVideoModel    string
	ArkVideoAPIPath         string
	ArkVideoResultAPIPath   string
	ArkVideoPollMaxAttempts int
	ArkVideoPollIntervalMs  int64
	ArkVideoDurationSeconds int
	ArkResponseFormat       string
	ArkSize                 string
	ArkStream               bool
	ArkWatermark            bool
	ArkSequentialImageGen   string
}

func getenv(key, def string) string {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	return v
}

func getenvInt(key string, def int) int {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return n
}

func getenvInt64(key string, def int64) int64 {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	n, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return def
	}
	return n
}

func getenvBool(key string, def bool) bool {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	b, err := strconv.ParseBool(v)
	if err != nil {
		return def
	}
	return b
}

// ArkImageModelOptionsEnv returns extra image model ids from ARK_IMAGE_MODEL_OPTIONS (comma-separated). Empty means caller uses Ark default only.
func ArkImageModelOptionsEnv() []string {
	return splitCommaNonEmpty(getenv("ARK_IMAGE_MODEL_OPTIONS", ""))
}

// ArkVideoModelOptionsEnv returns extra video model ids from ARK_VIDEO_MODEL_OPTIONS.
func ArkVideoModelOptionsEnv() []string {
	return splitCommaNonEmpty(getenv("ARK_VIDEO_MODEL_OPTIONS", ""))
}

func splitCommaNonEmpty(s string) []string {
	s = strings.TrimSpace(s)
	if s == "" {
		return nil
	}
	var out []string
	for _, p := range strings.Split(s, ",") {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}

func Load() *Config {
	c := &Config{
		ServerPort: getenvInt("SERVER_PORT", 8080),

		DBHost:     getenv("DB_HOST", "127.0.0.1"),
		DBPort:     getenvInt("DB_PORT", 3306),
		DBName:     getenv("DB_NAME", "aigc_manju"),
		DBUser:     getenv("DB_USER", "root"),
		DBPassword: getenv("DB_PASSWORD", "rootroot"),

		DataDir: getenv("AIGC_DATA_DIR", os.Getenv("HOME")+"/.aigcmanju"),

		AuthAccessToken:    getenv("AIGC_ACCESS_TOKEN", "dev-local-token"),
		AuthUserIDRequired: getenvBool("AIGC_USER_ID_REQUIRED", true),

		EncryptionKey: getenv("ENCRYPTION_KEY", "default-encryption-key-change-in-production"),

		PipelineVideoMaxParallel:    getenvInt("AIGC_PIPELINE_VIDEO_MAX_PARALLEL", 3),
		PipelineVideoPollIntervalMs: getenvInt64("AIGC_PIPELINE_VIDEO_POLL_INTERVAL_MS", 3000),
		PipelineVideoMaxRetries:     getenvInt("AIGC_PIPELINE_VIDEO_MAX_RETRIES", 2),

		OnelinkAIEnabled:                 getenvBool("AIGC_ONELINKAI_ENABLED", true),
		OnelinkAIBaseURL:                 getenv("AIGC_ONELINKAI_BASE_URL", "https://api.onelinkai.cloud"),
		OnelinkAIModelListRemote:         getenvBool("AIGC_ONELINKAI_MODEL_LIST_REMOTE_ENABLED", true),
		OnelinkAIModelListTimeoutMs:      getenvInt("AIGC_ONELINKAI_MODEL_LIST_TIMEOUT_MS", 8000),
		OnelinkAIModelListFallbackStatic: getenvBool("AIGC_ONELINKAI_MODEL_LIST_FALLBACK_STATIC", true),

		ComfyBaseURL: getenv("AIGC_COMFY_BASE_URL", "http://127.0.0.1:8188"),

		ArkBaseURL:              getenv("ARK_BASE_URL", "https://ark.cn-beijing.volces.com"),
		ArkAPIKey:               getenv("ARK_API_KEY", ""),
		ArkDefaultImageModel:    getenv("ARK_DEFAULT_IMAGE_MODEL", "doubao-seedream-5-0-260128"),
		ArkDefaultVideoModel:    getenv("ARK_DEFAULT_VIDEO_MODEL", "doubao-seedance-1-5-pro-251215"),
		ArkVideoAPIPath:         getenv("ARK_VIDEO_API_PATH", "/api/v3/contents/generations/tasks"),
		ArkVideoResultAPIPath:   getenv("ARK_VIDEO_RESULT_API_PATH", "/api/v3/contents/generations/tasks/{taskId}"),
		ArkVideoPollMaxAttempts: getenvInt("ARK_VIDEO_POLL_MAX_ATTEMPTS", 40),
		ArkVideoPollIntervalMs:  getenvInt64("ARK_VIDEO_POLL_INTERVAL_MS", 3000),
		ArkVideoDurationSeconds: getenvInt("ARK_VIDEO_DURATION_SECONDS", 5),
		ArkResponseFormat:       getenv("ARK_RESPONSE_FORMAT", "url"),
		ArkSize:                 getenv("ARK_SIZE", "2K"),
		ArkStream:               getenvBool("ARK_STREAM", false),
		ArkWatermark:            getenvBool("ARK_WATERMARK", true),
		ArkSequentialImageGen:   getenv("ARK_SEQUENTIAL_IMAGE_GENERATION", "disabled"),
	}
	raw := getenv("AIGC_CORS_ALLOWED_ORIGIN_PATTERNS", "http://localhost:*,http://127.0.0.1:*")
	for _, p := range strings.Split(raw, ",") {
		p = strings.TrimSpace(p)
		if p != "" {
			c.CORSAllowedOriginPatterns = append(c.CORSAllowedOriginPatterns, p)
		}
	}
	return c
}

func (c *Config) DSN() string {
	return c.DBUser + ":" + c.DBPassword + "@tcp(" + c.DBHost + ":" + strconv.Itoa(c.DBPort) + ")/" + c.DBName +
		"?charset=utf8mb4&parseTime=True&loc=Asia%2FShanghai"
}
