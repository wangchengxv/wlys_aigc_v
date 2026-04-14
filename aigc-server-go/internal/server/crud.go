package server

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/example/aigc-server-go/internal/api"
	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/jsonutil"
	"github.com/example/aigc-server-go/internal/model"
	"github.com/example/aigc-server-go/internal/service"
	"github.com/gin-gonic/gin"
)

func registerCRUD(authed *gin.RouterGroup, app *App) {
	authed.GET("/connections", func(c *gin.Context) {
		rows, err := app.Stores.AllConnections()
		if err != nil {
			c.JSON(http.StatusOK, api.Fail(500, err.Error()))
			return
		}
		var out []map[string]any
		for i := range rows {
			out = append(out, connectionToJSON(app, &rows[i]))
		}
		c.JSON(http.StatusOK, api.OK(out))
	})

	authed.POST("/connections/quick", func(c *gin.Context) {
		var req struct {
			Provider  string `json:"provider"`
			ModelName string `json:"modelName"`
			APIKey    string `json:"apiKey"`
			Enabled   *bool  `json:"enabled"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		row, err := app.Connections.QuickCreate(req.Provider, req.ModelName, req.APIKey, req.Enabled)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(connectionToJSON(app, row)))
	})

	authed.GET("/connections/:id", func(c *gin.Context) {
		row, err := app.Connections.Get(c.Param("id"))
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(connectionToJSON(app, row)))
	})

	authed.POST("/connections", func(c *gin.Context) {
		var req struct {
			Name     string         `json:"name"`
			Provider string         `json:"provider"`
			BaseURL  string         `json:"baseUrl"`
			APIKey   string         `json:"apiKey"`
			Enabled  bool           `json:"enabled"`
			Metadata map[string]any `json:"metadata"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		row, err := app.Connections.Create(req.Name, req.Provider, req.BaseURL, req.Enabled, req.APIKey, req.Metadata)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(connectionToJSON(app, row)))
	})

	authed.PUT("/connections/:id", func(c *gin.Context) {
		var req struct {
			Name     *string        `json:"name"`
			Provider *string        `json:"provider"`
			BaseURL  *string        `json:"baseUrl"`
			APIKey   *string        `json:"apiKey"`
			Enabled  *bool          `json:"enabled"`
			Metadata map[string]any `json:"metadata"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		row, err := app.Connections.Update(c.Param("id"), req.Name, req.Provider, req.BaseURL, req.Enabled, req.APIKey, req.Metadata)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(connectionToJSON(app, row)))
	})

	authed.DELETE("/connections/:id", func(c *gin.Context) {
		if err := app.Connections.Delete(c.Param("id")); err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OKNil())
	})

	authed.POST("/connections/:id/test", func(c *gin.Context) {
		ok, msg, models, err := app.Connections.Test(c.Param("id"))
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(map[string]any{"ok": ok, "message": msg, "models": models}))
	})

	authed.GET("/models", func(c *gin.Context) {
		rows, err := app.Stores.AllModels()
		if err != nil {
			c.JSON(http.StatusOK, api.Fail(500, err.Error()))
			return
		}
		var out []map[string]any
		for i := range rows {
			out = append(out, modelToJSON(&rows[i]))
		}
		c.JSON(http.StatusOK, api.OK(out))
	})

	authed.POST("/models/batch-import", func(c *gin.Context) {
		var req struct {
			ConnectionID string   `json:"connectionId"`
			ModelNames   []string `json:"modelNames"`
			Capabilities []string `json:"capabilities"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		rows, err := app.Models.BatchImport(req.ConnectionID, req.ModelNames, req.Capabilities)
		if err != nil {
			writeErr(c, err)
			return
		}
		var out []map[string]any
		for _, r := range rows {
			out = append(out, modelToJSON(r))
		}
		c.JSON(http.StatusOK, api.OK(out))
	})

	authed.GET("/models/:id", func(c *gin.Context) {
		row, err := app.Models.Get(c.Param("id"))
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(modelToJSON(row)))
	})

	authed.POST("/models", func(c *gin.Context) {
		var req struct {
			Name         string         `json:"name"`
			Provider     string         `json:"provider"`
			ModelName    string         `json:"modelName"`
			ConnectionID string         `json:"connectionId"`
			Enabled      bool           `json:"enabled"`
			Metadata     map[string]any `json:"metadata"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		row, err := app.Models.Create(req.Name, req.Provider, req.ModelName, req.ConnectionID, req.Enabled, req.Metadata)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(modelToJSON(row)))
	})

	authed.PUT("/models/:id", func(c *gin.Context) {
		var req struct {
			Name         *string        `json:"name"`
			Provider     *string        `json:"provider"`
			ModelName    *string        `json:"modelName"`
			ConnectionID *string        `json:"connectionId"`
			Enabled      *bool          `json:"enabled"`
			Metadata     map[string]any `json:"metadata"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		row, err := app.Models.Update(c.Param("id"), req.Name, req.Provider, req.ModelName, req.ConnectionID, req.Enabled, req.Metadata)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(modelToJSON(row)))
	})

	authed.POST("/models/:id/probe", func(c *gin.Context) {
		ok, msg, err := app.Models.Probe(c.Param("id"))
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(map[string]any{"ok": ok, "message": msg}))
	})

	authed.DELETE("/models/:id", func(c *gin.Context) {
		if err := app.Models.Delete(c.Param("id")); err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OKNil())
	})

	authed.GET("/preset-models", func(c *gin.Context) {
		c.JSON(http.StatusOK, api.OK(presetModelsPayload()))
	})

	authed.GET("/provider-catalog", func(c *gin.Context) {
		var entries []map[string]any
		for _, p := range app.Catalog.List() {
			entries = append(entries, providerCatalogEntry(p))
		}
		c.JSON(http.StatusOK, api.OK(map[string]any{"providers": entries}))
	})

	authed.GET("/provider-catalog/oauth-notes", func(c *gin.Context) {
		c.JSON(http.StatusOK, api.OK(map[string]string{
			"message": "GitHub Copilot、CherryIN 等依赖 Electron/OAuth 桌面流程的提供商未在本 Web 网关实现；请使用 Cherry Studio 客户端或接入兼容 OpenAI 的代理。",
			"ovms":    "OVMS 模型下载与本地推理需 OpenVINO 运行时，本 Web 应用不提供与 Cherry 桌面相同的下载向导；请在服务器或本地自行部署模型。",
			"copilot": "GitHub Copilot 需 OAuth；Web 侧请改用 OpenAI 兼容中转或官方 API Key。",
		}))
	})
}

func presetModelsPayload() map[string]any {
	var models []map[string]any
	seen := map[string]struct{}{}
	var providers []string
	for _, p := range service.PresetModels {
		models = append(models, map[string]any{
			"provider":     p.Provider,
			"modelName":    p.ModelName,
			"baseUrl":      p.BaseURL,
			"displayName":  p.DisplayName,
			"capabilities": p.Caps,
		})
		if _, ok := seen[p.Provider]; !ok {
			seen[p.Provider] = struct{}{}
			providers = append(providers, p.Provider)
		}
	}
	return map[string]any{"models": models, "providers": providers}
}

func providerCatalogEntry(p *catalog.Provider) map[string]any {
	img := p.ImageGenerationPath != ""
	vid := p.VideoSubmitPath != ""
	return map[string]any{
		"key":                 p.Key,
		"displayName":         p.DisplayName,
		"defaultBaseUrl":      p.DefaultBaseURL,
		"authMode":            p.AuthMode.JavaName(),
		"apiFormat":           p.APIFormat,
		"gatewayKind":         p.Kind,
		"textProxySupported":  p.TextProxySupported,
		"imageProxySupported": img,
		"videoProxySupported": vid,
		"staticModels":        p.StaticModels,
	}
}

func connectionToJSON(app *App, c *model.ConnectionConfig) map[string]any {
	meta := service.MaskForResponse(jsonutil.MapFromJSON(c.MetadataJSON))
	plain := ""
	if c.EncryptedAPIKey != "" {
		plain, _ = app.Crypto.Decrypt(c.EncryptedAPIKey)
	}
	masked := crypto.Mask(plain)
	return map[string]any{
		"id": c.ID, "name": c.Name, "provider": c.Provider, "baseUrl": c.BaseURL,
		"apiKeyMasked": masked, "hasApiKey": plain != "",
		"enabled": c.Enabled, "metadata": meta,
		"createdAt": c.CreatedAt.Format(time.RFC3339Nano),
		"updatedAt": c.UpdatedAt.Format(time.RFC3339Nano),
	}
}

func modelToJSON(m *model.ModelConfig) map[string]any {
	var meta map[string]any
	_ = json.Unmarshal([]byte(m.MetadataJSON), &meta)
	return map[string]any{
		"id": m.ID, "name": m.Name, "provider": m.Provider, "modelName": m.ModelName,
		"connectionId": m.ConnectionID, "enabled": m.Enabled, "metadata": meta,
		"createdAt": m.CreatedAt.Format(time.RFC3339Nano),
		"updatedAt": m.UpdatedAt.Format(time.RFC3339Nano),
	}
}
