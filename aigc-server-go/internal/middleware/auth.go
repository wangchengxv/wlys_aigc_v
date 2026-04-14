package middleware

import (
	"net/http"
	"strings"

	"github.com/example/aigc-server-go/internal/api"
	"github.com/example/aigc-server-go/internal/config"
	"github.com/gin-gonic/gin"
)

const CtxUserID = "userId"

// RequireAPIToken validates Bearer / x-aigc-token for /api/** routes (aligned with RequestAuthService).
func RequireAPIToken(cfg *config.Config) gin.HandlerFunc {
	expected := strings.TrimSpace(cfg.AuthAccessToken)
	return func(c *gin.Context) {
		if expected == "" {
			c.JSON(http.StatusOK, api.Fail(500, "服务未配置访问令牌，请联系管理员"))
			c.Abort()
			return
		}
		token := extractToken(c.GetHeader("Authorization"), c.GetHeader("x-aigc-token"))
		if token != expected {
			c.JSON(http.StatusOK, api.Fail(401, "未授权访问"))
			c.Abort()
			return
		}
		c.Next()
	}
}

// RequireUserID requires x-user-id when configured (aligned with RequestAuthService.requireUserId).
func RequireUserID(cfg *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		uid := strings.TrimSpace(c.GetHeader("x-user-id"))
		if cfg.AuthUserIDRequired && uid == "" {
			c.JSON(http.StatusOK, api.Fail(401, "缺少用户标识，请设置 x-user-id"))
			c.Abort()
			return
		}
		if uid == "" {
			c.Set(CtxUserID, "anonymous")
		} else {
			c.Set(CtxUserID, uid)
		}
		c.Next()
	}
}

func extractToken(authorization, xAigcToken string) string {
	if authorization != "" {
		auth := strings.TrimSpace(authorization)
		if len(auth) >= 7 && strings.EqualFold(auth[:7], "Bearer ") {
			return strings.TrimSpace(auth[7:])
		}
		if auth != "" {
			return auth
		}
	}
	return strings.TrimSpace(xAigcToken)
}

// RouterAPIAuth validates router API keys for /v1/* OpenAI-compatible proxy (handled in service layer).
func RouterBearerOnly() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Next()
	}
}
