package service

import (
	"context"
	"fmt"
	"net"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/errs"
	"github.com/example/aigc-server-go/internal/gateway"
	"github.com/example/aigc-server-go/internal/jsonutil"
	"github.com/example/aigc-server-go/internal/metadata"
	"github.com/example/aigc-server-go/internal/model"
	"github.com/example/aigc-server-go/internal/repo"
	"github.com/google/uuid"
)

// ConnectionAdmin implements Java ConnectionConfigService.
type ConnectionAdmin struct {
	Store   *repo.Stores
	Cat     *catalog.Catalog
	Crypto  *crypto.Service
	GW      *gateway.HTTPGateway
	Routing *RouterRouting
}

func strMeta(v any) string {
	if v == nil {
		return ""
	}
	return strings.TrimSpace(fmt.Sprint(v))
}

func (s *ConnectionAdmin) validateCreate(def *catalog.Provider, rawKey string, meta map[string]any) error {
	switch def.Kind {
	case catalog.KindBedrock:
		if strings.TrimSpace(rawKey) == "" {
			return errs.New(400, "请填写 AWS Secret Access Key")
		}
		if strMeta(meta[metadata.AWSAccessKeyID]) == "" {
			return errs.New(400, "请填写 AWS Access Key ID（metadata.awsAccessKeyId）")
		}
		if strMeta(meta[metadata.AWSRegion]) == "" {
			return errs.New(400, "请填写 AWS Region（metadata.region）")
		}
		return nil
	case catalog.KindVertex:
		if strMeta(meta[metadata.VertexProject]) == "" || strMeta(meta[metadata.VertexLocation]) == "" {
			return errs.New(400, "请填写 Vertex 项目 ID 与区域（metadata）")
		}
		if strMeta(meta[metadata.VertexSAJSON]) == "" {
			return errs.New(400, "请填写 Service Account JSON")
		}
		return nil
	default:
		if def.AuthMode != catalog.AuthNone && strings.TrimSpace(rawKey) == "" {
			return errs.New(400, "API Key 不能为空")
		}
	}
	return nil
}

func (s *ConnectionAdmin) encryptStoredAPIKey(def *catalog.Provider, rawKey string) (string, error) {
	rawKey = strings.TrimSpace(rawKey)
	if def.Kind == catalog.KindBedrock {
		if rawKey == "" {
			return "", nil
		}
		return s.Crypto.Encrypt(rawKey)
	}
	if def.AuthMode == catalog.AuthNone {
		return "", nil
	}
	if rawKey == "" {
		return "", nil
	}
	return s.Crypto.Encrypt(rawKey)
}

// Create mirrors ConnectionConfigService.create.
func (s *ConnectionAdmin) Create(name, providerKey, baseURL string, enabled bool, apiKey string, meta map[string]any) (*model.ConnectionConfig, error) {
	def, err := s.Cat.Require(providerKey)
	if err != nil {
		return nil, err
	}
	if err := s.validateCreate(def, apiKey, meta); err != nil {
		return nil, err
	}
	normBase, err := normalizeBaseURL(baseURL, def)
	if err != nil {
		return nil, err
	}
	metaStored := NormalizeIncoming(meta, s.Crypto)
	enc, err := s.encryptStoredAPIKey(def, apiKey)
	if err != nil {
		return nil, err
	}
	now := time.Now()
	cfg := &model.ConnectionConfig{
		ID:              uuid.NewString(),
		Name:            strings.TrimSpace(name),
		Provider:        def.Key,
		BaseURL:         normBase,
		EncryptedAPIKey: enc,
		MetadataJSON:    jsonutil.ToJSONMap(metaStored),
		Enabled:         enabled,
		CreatedAt:       now,
		UpdatedAt:       now,
	}
	if err := s.Store.SaveConnection(cfg); err != nil {
		return nil, err
	}
	_ = s.Routing.AppendConnectionIfAbsent(cfg.ID)
	return cfg, nil
}

// QuickCreate mirrors quickCreate.
func (s *ConnectionAdmin) QuickCreate(provider, modelName, apiKey string, enabled *bool) (*model.ConnectionConfig, error) {
	preset := FindPreset(provider, modelName)
	if preset == nil {
		return nil, errs.New(400, "该模型不在预置库中，请使用高级模式配置")
	}
	def, err := s.Cat.Require(preset.Provider)
	if err != nil {
		return nil, err
	}
	normBase, err := normalizeBaseURL(preset.BaseURL, def)
	if err != nil {
		return nil, err
	}
	enc, err := s.Crypto.Encrypt(strings.TrimSpace(apiKey))
	if err != nil {
		return nil, err
	}
	en := true
	if enabled != nil {
		en = *enabled
	}
	name := preset.Provider + " - " + preset.DisplayName
	now := time.Now()
	cfg := &model.ConnectionConfig{
		ID:              uuid.NewString(),
		Name:            name,
		Provider:        def.Key,
		BaseURL:         normBase,
		EncryptedAPIKey: enc,
		MetadataJSON:    "{}",
		Enabled:         en,
		CreatedAt:       now,
		UpdatedAt:       now,
	}
	if err := s.Store.SaveConnection(cfg); err != nil {
		return nil, err
	}
	_ = s.Routing.AppendConnectionIfAbsent(cfg.ID)

	meta := (&ModelCapability{}).MergeCapabilities(map[string]any{}, preset.Caps)
	mc := &model.ModelConfig{
		ID:           uuid.NewString(),
		Name:         preset.DisplayName,
		Provider:     def.Key,
		ModelName:    preset.ModelName,
		ConnectionID: cfg.ID,
		Enabled:      true,
		MetadataJSON: jsonutil.ToJSONMap(meta),
		CreatedAt:    now,
		UpdatedAt:    now,
	}
	_ = s.Store.SaveModel(mc)
	return cfg, nil
}

// Get returns connection or 404.
func (s *ConnectionAdmin) Get(id string) (*model.ConnectionConfig, error) {
	c, err := s.Store.GetConnection(id)
	if err != nil {
		return nil, errs.New(404, "连接配置不存在")
	}
	return c, nil
}

// Update mirrors update().
func (s *ConnectionAdmin) Update(id string, name *string, providerKey *string, baseURL *string, enabled *bool, apiKey *string, metaPatch map[string]any) (*model.ConnectionConfig, error) {
	cfg, err := s.Store.GetConnection(id)
	if err != nil {
		return nil, errs.New(404, "连接配置不存在")
	}
	prov := cfg.Provider
	if providerKey != nil && strings.TrimSpace(*providerKey) != "" {
		def, err := s.Cat.Require(*providerKey)
		if err != nil {
			return nil, err
		}
		prov = def.Key
	}
	def, _ := s.Cat.Require(prov)
	if name != nil && strings.TrimSpace(*name) != "" {
		cfg.Name = strings.TrimSpace(*name)
	}
	cfg.Provider = prov
	if baseURL != nil && strings.TrimSpace(*baseURL) != "" {
		nb, err := normalizeBaseURL(*baseURL, def)
		if err != nil {
			return nil, err
		}
		cfg.BaseURL = nb
	} else if strings.TrimSpace(cfg.BaseURL) == "" {
		cfg.BaseURL = def.DefaultBaseURL
	}
	if enabled != nil {
		cfg.Enabled = *enabled
	}
	if apiKey != nil && strings.TrimSpace(*apiKey) != "" {
		enc, err := s.encryptStoredAPIKey(def, *apiKey)
		if err != nil {
			return nil, err
		}
		cfg.EncryptedAPIKey = enc
	}
	if metaPatch != nil {
		merged := MergeMetadata(cfg.MetadataJSON, metaPatch, s.Crypto)
		cfg.MetadataJSON = jsonutil.ToJSONMap(merged)
	}
	cfg.UpdatedAt = time.Now()
	if err := s.Store.SaveConnection(cfg); err != nil {
		return nil, err
	}
	return cfg, nil
}

// Delete mirrors delete().
func (s *ConnectionAdmin) Delete(id string) error {
	if _, err := s.Store.GetConnection(id); err != nil {
		return errs.New(404, "连接配置不存在")
	}
	models, err := s.Store.AllModels()
	if err != nil {
		return err
	}
	for _, m := range models {
		if m.ConnectionID == id {
			_ = s.Store.DeleteModel(m.ID)
		}
	}
	if err := s.Store.DeleteConnection(id); err != nil {
		return err
	}
	_ = s.Routing.RemoveConnection(id)
	return nil
}

// Test mirrors test() — lists remote models via gateway (Biz failures only for missing connection).
func (s *ConnectionAdmin) Test(id string) (ok bool, message string, models []string, err error) {
	cfg, err := s.Store.GetConnection(id)
	if err != nil {
		return false, "", nil, errs.New(404, "连接配置不存在")
	}
	def, err := s.Cat.Require(cfg.Provider)
	if err != nil {
		return false, err.Error(), nil, nil
	}
	meta := jsonutil.MapFromJSON(cfg.MetadataJSON)
	plainMeta := DecryptForUse(meta, s.Crypto)
	apiKey := s.resolvePlainAPIKey(def, cfg, plainMeta)
	ctx := context.Background()
	list, err := s.GW.ListModels(ctx, def, cfg.BaseURL, apiKey, plainMeta, 8*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return false, pe.Message, nil, nil
		}
		return false, err.Error(), nil, nil
	}
	return true, "连接测试通过", list, nil
}

func (s *ConnectionAdmin) resolvePlainAPIKey(def *catalog.Provider, cfg *model.ConnectionConfig, metaPlain map[string]any) string {
	if def.Kind == catalog.KindBedrock {
		if cfg.EncryptedAPIKey == "" {
			return ""
		}
		p, err := s.Crypto.Decrypt(cfg.EncryptedAPIKey)
		if err != nil {
			return ""
		}
		return p
	}
	if def.Kind == catalog.KindVertex {
		return ""
	}
	if def.AuthMode == catalog.AuthNone {
		return ""
	}
	if cfg.EncryptedAPIKey == "" {
		return ""
	}
	p, err := s.Crypto.Decrypt(cfg.EncryptedAPIKey)
	if err != nil {
		return ""
	}
	return p
}

func normalizeBaseURL(raw string, def *catalog.Provider) (string, error) {
	normalized := strings.TrimSpace(raw)
	if normalized == "" {
		normalized = def.DefaultBaseURL
	}
	u, err := url.Parse(normalized)
	if err != nil {
		return "", errs.New(400, "Base URL 格式不正确")
	}
	scheme := strings.ToLower(u.Scheme)
	if scheme != "https" {
		return "", errs.New(400, "Base URL 仅支持 https 协议")
	}
	host := u.Hostname()
	if host == "" {
		return "", errs.New(400, "Base URL 缺少主机名")
	}
	if isPrivateOrLocalHost(host) {
		return "", errs.New(400, "Base URL 不允许使用本地或内网地址")
	}
	return u.String(), nil
}

func isPrivateOrLocalHost(host string) bool {
	h := strings.ToLower(strings.TrimSpace(host))
	if h == "localhost" || strings.HasSuffix(h, ".localhost") {
		return true
	}
	if ip := net.ParseIP(h); ip != nil {
		return ip.IsLoopback() || ip.IsPrivate() || ip.IsLinkLocalUnicast()
	}
	if strings.HasPrefix(h, "10.") {
		return true
	}
	if strings.HasPrefix(h, "127.") {
		return true
	}
	if strings.HasPrefix(h, "169.254.") {
		return true
	}
	if strings.HasPrefix(h, "192.168.") {
		return true
	}
	if strings.HasPrefix(h, "172.") {
		parts := strings.Split(h, ".")
		if len(parts) >= 2 {
			if sec, err := strconv.Atoi(parts[1]); err == nil && sec >= 16 && sec <= 31 {
				return true
			}
		}
	}
	return false
}
