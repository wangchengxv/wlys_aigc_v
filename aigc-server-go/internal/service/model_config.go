package service

import (
	"context"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/errs"
	"github.com/example/aigc-server-go/internal/gateway"
	"github.com/example/aigc-server-go/internal/jsonutil"
	"github.com/example/aigc-server-go/internal/model"
	"github.com/example/aigc-server-go/internal/repo"
	"github.com/google/uuid"
)

// ModelAdmin implements Java ModelConfigService.
type ModelAdmin struct {
	Store  *repo.Stores
	Cat    *catalog.Catalog
	Crypto *crypto.Service
	GW     *gateway.HTTPGateway
	Cap    *ModelCapability
}

func (s *ModelAdmin) validateConnectionID(connectionID string) error {
	if _, err := s.Store.GetConnection(connectionID); err != nil {
		return errs.New(400, "关联连接配置不存在")
	}
	return nil
}

// Create creates a model_config row.
func (s *ModelAdmin) Create(name, providerKey, modelName, connectionID string, enabled bool, meta map[string]any) (*model.ModelConfig, error) {
	if err := s.validateConnectionID(connectionID); err != nil {
		return nil, err
	}
	def, err := s.Cat.Require(providerKey)
	if err != nil {
		return nil, err
	}
	norm := s.Cap.NormalizeModelMetadata(meta, def.Key, modelName)
	now := time.Now()
	mc := &model.ModelConfig{
		ID:           uuid.NewString(),
		Name:         strings.TrimSpace(name),
		Provider:     def.Key,
		ModelName:    strings.TrimSpace(modelName),
		ConnectionID: connectionID,
		Enabled:      enabled,
		MetadataJSON: jsonutil.ToJSONMap(norm),
		CreatedAt:    now,
		UpdatedAt:    now,
	}
	if err := s.Store.SaveModel(mc); err != nil {
		return nil, err
	}
	return mc, nil
}

// BatchImport mirrors batchImport.
func (s *ModelAdmin) BatchImport(connectionID string, modelNames []string, capabilities []string) ([]*model.ModelConfig, error) {
	if err := s.validateConnectionID(connectionID); err != nil {
		return nil, err
	}
	conn, err := s.Store.GetConnection(connectionID)
	if err != nil {
		return nil, errs.New(400, "关联连接不存在")
	}
	def, err := s.Cat.Require(conn.Provider)
	if err != nil {
		return nil, err
	}
	caps := capabilities
	if len(caps) == 0 {
		caps = []string{"text"}
	}
	var out []*model.ModelConfig
	for _, raw := range modelNames {
		mn := strings.TrimSpace(raw)
		if mn == "" {
			continue
		}
		meta := s.Cap.MergeCapabilities(map[string]any{}, caps)
		meta = s.Cap.NormalizeModelMetadata(meta, def.Key, mn)
		now := time.Now()
		mc := &model.ModelConfig{
			ID:           uuid.NewString(),
			Name:         mn,
			Provider:     def.Key,
			ModelName:    mn,
			ConnectionID: connectionID,
			Enabled:      true,
			MetadataJSON: jsonutil.ToJSONMap(meta),
			CreatedAt:    now,
			UpdatedAt:    now,
		}
		if err := s.Store.SaveModel(mc); err != nil {
			return nil, err
		}
		out = append(out, mc)
	}
	return out, nil
}

// Get returns model or 404.
func (s *ModelAdmin) Get(id string) (*model.ModelConfig, error) {
	m, err := s.Store.GetModel(id)
	if err != nil {
		return nil, errs.New(404, "模型配置不存在")
	}
	return m, nil
}

// Update updates model.
func (s *ModelAdmin) Update(id string, name *string, providerKey *string, modelName *string, connectionID *string, enabled *bool, meta map[string]any) (*model.ModelConfig, error) {
	mc, err := s.Store.GetModel(id)
	if err != nil {
		return nil, errs.New(404, "模型配置不存在")
	}
	cid := mc.ConnectionID
	if connectionID != nil && strings.TrimSpace(*connectionID) != "" {
		cid = strings.TrimSpace(*connectionID)
		if err := s.validateConnectionID(cid); err != nil {
			return nil, err
		}
	}
	prov := mc.Provider
	if providerKey != nil && strings.TrimSpace(*providerKey) != "" {
		def, err := s.Cat.Require(*providerKey)
		if err != nil {
			return nil, err
		}
		prov = def.Key
	}
	mn := mc.ModelName
	if modelName != nil && strings.TrimSpace(*modelName) != "" {
		mn = strings.TrimSpace(*modelName)
	}
	if name != nil && strings.TrimSpace(*name) != "" {
		mc.Name = strings.TrimSpace(*name)
	}
	mc.Provider = prov
	mc.ModelName = mn
	mc.ConnectionID = cid
	if enabled != nil {
		mc.Enabled = *enabled
	}
	baseMeta := jsonutil.MapFromJSON(mc.MetadataJSON)
	if meta != nil {
		mc.MetadataJSON = jsonutil.ToJSONMap(s.Cap.NormalizeModelMetadata(meta, prov, mn))
	} else {
		mc.MetadataJSON = jsonutil.ToJSONMap(s.Cap.NormalizeModelMetadata(baseMeta, prov, mn))
	}
	mc.UpdatedAt = time.Now()
	if err := s.Store.SaveModel(mc); err != nil {
		return nil, err
	}
	return mc, nil
}

// Delete removes model.
func (s *ModelAdmin) Delete(id string) error {
	if _, err := s.Store.GetModel(id); err != nil {
		return errs.New(404, "模型配置不存在")
	}
	return s.Store.DeleteModel(id)
}

// Probe invokes minimal chat (Java ModelConfigService.probe).
func (s *ModelAdmin) Probe(modelID string) (bool, string, error) {
	mc, err := s.Store.GetModel(modelID)
	if err != nil {
		return false, "", errs.New(404, "模型配置不存在")
	}
	conn, err := s.Store.GetConnection(mc.ConnectionID)
	if err != nil {
		return false, "", errs.New(400, "关联连接不存在")
	}
	def, err := s.Cat.Require(conn.Provider)
	if err != nil {
		return false, "", err
	}
	meta := jsonutil.MapFromJSON(conn.MetadataJSON)
	plainMeta := DecryptForUse(meta, s.Crypto)
	apiKey := probeAPIKey(s.Crypto, def, conn)
	payload := probePayload(def, mc.ModelName)
	ctx := context.Background()
	_, err = s.GW.InvokeChat(ctx, def, conn.BaseURL, apiKey, plainMeta, payload, 20*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return false, pe.Message, nil
		}
		return false, err.Error(), nil
	}
	return true, "探测成功", nil
}

func probeAPIKey(cr *crypto.Service, def *catalog.Provider, conn *model.ConnectionConfig) string {
	if def.Kind == catalog.KindVertex {
		return ""
	}
	if def.Kind == catalog.KindBedrock {
		if conn.EncryptedAPIKey == "" {
			return ""
		}
		p, err := cr.Decrypt(conn.EncryptedAPIKey)
		if err != nil {
			return ""
		}
		return p
	}
	if def.AuthMode == catalog.AuthNone {
		return ""
	}
	if conn.EncryptedAPIKey == "" {
		return ""
	}
	p, err := cr.Decrypt(conn.EncryptedAPIKey)
	if err != nil {
		return ""
	}
	return p
}

func probePayload(def *catalog.Provider, modelName string) map[string]any {
	if strings.EqualFold(def.APIFormat, "anthropic") {
		return map[string]any{
			"model":      modelName,
			"max_tokens": 16,
			"messages":   []any{map[string]any{"role": "user", "content": "ping"}},
		}
	}
	return map[string]any{
		"model":      modelName,
		"max_tokens": 16,
		"messages":   []any{map[string]any{"role": "user", "content": "ping"}},
	}
}
