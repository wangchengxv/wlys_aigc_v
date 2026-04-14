package middleware

import (
	"net/http"
	"strings"

	"github.com/example/aigc-server-go/internal/config"
	"github.com/gin-gonic/gin"
)

// CORSAPI mirrors WebConfig: only /api/** with allowedOriginPatterns.
func CORSAPI(cfg *config.Config) gin.HandlerFunc {
	patterns := cfg.CORSAllowedOriginPatterns
	return func(c *gin.Context) {
		path := c.Request.URL.Path
		if !strings.HasPrefix(path, "/api/") {
			c.Next()
			return
		}
		origin := c.GetHeader("Origin")
		if origin != "" && originAllowed(origin, patterns) {
			c.Header("Access-Control-Allow-Origin", origin)
			c.Header("Access-Control-Allow-Credentials", "true")
		}
		c.Header("Access-Control-Allow-Methods", "*")
		c.Header("Access-Control-Allow-Headers", "*")
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	}
}

func originAllowed(origin string, patterns []string) bool {
	for _, p := range patterns {
		if patternMatch(p, origin) {
			return true
		}
	}
	return false
}

// Minimal glob: * matches within one path segment (enough for http://localhost:* style).
func patternMatch(pattern, origin string) bool {
	if pattern == origin {
		return true
	}
	if !strings.Contains(pattern, "*") {
		return false
	}
	// Split scheme://rest
	ps := strings.SplitN(pattern, "://", 2)
	os := strings.SplitN(origin, "://", 2)
	if len(ps) != 2 || len(os) != 2 {
		return false
	}
	if ps[0] != os[0] {
		return false
	}
	phost := ps[1]
	ohost := os[1]
	if strings.HasSuffix(phost, "*") {
		prefix := strings.TrimSuffix(phost, "*")
		return strings.HasPrefix(ohost, prefix)
	}
	return phost == ohost
}
