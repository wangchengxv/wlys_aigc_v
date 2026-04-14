package server

import (
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"

	"github.com/gin-gonic/gin"
)

const comfyPrefix = "/api/comfy"

// RegisterComfy mirrors ComfyProxyController.
func RegisterComfy(r *gin.Engine, baseURL string) {
	base := strings.TrimSpace(baseURL)
	if base == "" {
		base = "http://127.0.0.1:8188"
	}
	base = strings.TrimRight(base, "/")
	target, err := url.Parse(base)
	if err != nil {
		return
	}
	proxy := &httputil.ReverseProxy{
		Director: func(req *http.Request) {
			p := strings.TrimPrefix(req.URL.Path, comfyPrefix)
			if p == "" {
				p = "/"
			}
			if !strings.HasPrefix(p, "/") {
				p = "/" + p
			}
			req.URL.Scheme = target.Scheme
			req.URL.Host = target.Host
			req.URL.Path = p
			req.Host = target.Host
		},
	}
	handler := func(c *gin.Context) {
		proxy.ServeHTTP(c.Writer, c.Request)
	}
	r.Any(comfyPrefix, handler)
	r.Any(comfyPrefix+"/*path", handler)
}
