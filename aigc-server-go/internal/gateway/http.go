package gateway

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/metadata"
)

func str(v any) string {
	if v == nil {
		return ""
	}
	return strings.TrimSpace(fmt.Sprint(v))
}

// HTTPGateway mirrors ProviderHttpGateway (non-streaming core).
type HTTPGateway struct {
	HTTP    *http.Client
	Bedrock BedrockGateway
	Vertex  *VertexGateway
}

func NewHTTPGateway() *HTTPGateway {
	return &HTTPGateway{
		HTTP:   &http.Client{Timeout: 120 * time.Second},
		Vertex: &VertexGateway{HTTP: &http.Client{Timeout: 120 * time.Second}},
	}
}

func (g *HTTPGateway) InvokeChat(ctx context.Context, def *catalog.Provider, baseURL, apiKey string, meta map[string]any, payload map[string]any, timeout time.Duration) (map[string]any, error) {
	if meta == nil {
		meta = map[string]any{}
	}
	switch def.Kind {
	case catalog.KindBedrock:
		region := str(meta[metadata.AWSRegion])
		accessKey := str(meta[metadata.AWSAccessKeyID])
		sessionTok := str(meta[metadata.AWSSessionToken])
		return g.Bedrock.Converse(ctx, region, accessKey, apiKey, sessionTok, payload)
	case catalog.KindVertex:
		project := str(meta[metadata.VertexProject])
		loc := str(meta[metadata.VertexLocation])
		sa := str(meta[metadata.VertexSAJSON])
		return g.Vertex.GenerateContent(ctx, project, loc, sa, payload)
	default:
		if def.ChatPath == "" {
			return nil, NewProviderError(400, "当前提供商不支持文本代理")
		}
		return g.PostJSON(ctx, baseURL, def.ChatPath, def, apiKey, meta, payload, timeout)
	}
}

func (g *HTTPGateway) PostJSON(ctx context.Context, baseURL, path string, def *catalog.Provider, apiKey string, meta map[string]any, payload map[string]any, timeout time.Duration) (map[string]any, error) {
	u := buildURI(baseURL, path, meta)
	body, err := json.Marshal(payload)
	if err != nil {
		return nil, NewProviderError(502, "序列化请求失败")
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, u, bytes.NewReader(body))
	if err != nil {
		return nil, NewProviderError(502, err.Error())
	}
	req.Header.Set("Content-Type", "application/json")
	applyHeaders(req, def, apiKey, meta)
	client := g.HTTP
	if client == nil {
		client = http.DefaultClient
	}
	resp, err := client.Do(req)
	if err != nil {
		return nil, NewProviderError(502, "模型服务调用失败")
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 400 {
		return nil, NewProviderError(resp.StatusCode, extractErrJSON(string(b)))
	}
	var out map[string]any
	if err := json.Unmarshal(b, &out); err != nil {
		return nil, NewProviderError(502, "模型服务返回了无法解析的数据")
	}
	return out, nil
}

func (g *HTTPGateway) GetJSON(ctx context.Context, baseURL, path string, def *catalog.Provider, apiKey string, meta map[string]any, timeout time.Duration) (map[string]any, error) {
	u := buildURI(baseURL, path, meta)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, NewProviderError(502, err.Error())
	}
	applyHeaders(req, def, apiKey, meta)
	client := g.HTTP
	if client == nil {
		client = http.DefaultClient
	}
	resp, err := client.Do(req)
	if err != nil {
		return nil, NewProviderError(502, "模型服务调用失败")
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 400 {
		return nil, NewProviderError(resp.StatusCode, extractErrJSON(string(b)))
	}
	var out map[string]any
	if err := json.Unmarshal(b, &out); err != nil {
		return nil, NewProviderError(502, "模型服务返回了无法解析的数据")
	}
	return out, nil
}

func (g *HTTPGateway) GenerateImage(ctx context.Context, def *catalog.Provider, baseURL, apiKey string, payload map[string]any, timeout time.Duration) (map[string]any, error) {
	if def.ImageGenerationPath == "" {
		return nil, NewProviderError(400, "当前提供商未配置图片接口")
	}
	return g.PostJSON(ctx, baseURL, def.ImageGenerationPath, def, apiKey, nil, payload, timeout)
}

func (g *HTTPGateway) SubmitVideoTask(ctx context.Context, def *catalog.Provider, baseURL, apiKey string, meta map[string]any, payload map[string]any, timeout time.Duration) (map[string]any, error) {
	if def.VideoSubmitPath == "" {
		return nil, NewProviderError(400, "当前提供商未配置视频接口")
	}
	if def.VideoSubmitMultipart {
		return g.submitVideoMultipart(ctx, def, baseURL, apiKey, meta, payload, timeout)
	}
	return g.PostJSON(ctx, baseURL, def.VideoSubmitPath, def, apiKey, meta, payload, timeout)
}

func (g *HTTPGateway) QueryVideoTask(ctx context.Context, def *catalog.Provider, baseURL, apiKey, taskID string, timeout time.Duration) (map[string]any, error) {
	if def.VideoResultPath == "" {
		return nil, NewProviderError(400, "当前提供商未配置视频查询接口")
	}
	path := strings.ReplaceAll(def.VideoResultPath, "{taskId}", url.QueryEscape(taskID))
	statusBase := baseURL
	if def.VideoTaskStatusBaseURL != "" {
		statusBase = strings.TrimRight(def.VideoTaskStatusBaseURL, "/")
	}
	return g.GetJSON(ctx, statusBase, path, def, apiKey, nil, timeout)
}

func (g *HTTPGateway) submitVideoMultipart(ctx context.Context, def *catalog.Provider, baseURL, apiKey string, meta map[string]any, payload map[string]any, timeout time.Duration) (map[string]any, error) {
	img := str(payload["image"])
	if !strings.HasPrefix(img, "http://") && !strings.HasPrefix(img, "https://") {
		return nil, NewProviderError(400, "图生视频 image 须为 http(s) 图片地址")
	}
	client := g.HTTP
	if client == nil {
		client = http.DefaultClient
	}
	imgResp, err := client.Get(img)
	if err != nil || imgResp.StatusCode >= 400 {
		return nil, NewProviderError(502, "下载参考图失败")
	}
	defer imgResp.Body.Close()
	imgBytes, _ := io.ReadAll(imgResp.Body)
	mt := "application/octet-stream"
	if ct := imgResp.Header.Get("Content-Type"); ct != "" {
		mt = strings.TrimSpace(strings.Split(ct, ";")[0])
	}
	fname := "image.jpg"
	if u, err := url.Parse(img); err == nil && u.Path != "" {
		fname = pathBaseName(u.Path)
	}
	var buf bytes.Buffer
	w := multipart.NewWriter(&buf)
	_ = w.WriteField("prompt", str(payload["prompt"]))
	_ = w.WriteField("model", str(payload["model"]))
	_ = w.WriteField("num_inference_steps", str(payload["num_inference_steps"]))
	_ = w.WriteField("num_frames", str(payload["num_frames"]))
	part, _ := w.CreateFormFile("image", fname)
	_, _ = part.Write(imgBytes)
	_ = w.Close()

	u := buildURI(baseURL, def.VideoSubmitPath, meta)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, u, &buf)
	if err != nil {
		return nil, NewProviderError(502, err.Error())
	}
	req.Header.Set("Content-Type", w.FormDataContentType())
	applyHeaders(req, def, apiKey, meta)
	resp, err := client.Do(req)
	if err != nil {
		return nil, NewProviderError(502, "模型服务调用失败")
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 400 {
		return nil, NewProviderError(resp.StatusCode, extractErrJSON(string(b)))
	}
	var out map[string]any
	if err := json.Unmarshal(b, &out); err != nil {
		return nil, NewProviderError(502, "模型服务返回了无法解析的数据")
	}
	_ = mt
	return out, nil
}

func pathBaseName(p string) string {
	p = strings.TrimSuffix(p, "/")
	i := strings.LastIndex(p, "/")
	if i < 0 {
		return p
	}
	return p[i+1:]
}

func trimTrailingSlash(v string) string {
	v = strings.TrimSpace(v)
	if v == "" {
		return ""
	}
	if strings.HasSuffix(v, "/") {
		return v[:len(v)-1]
	}
	return v
}

func buildURI(baseURL, path string, meta map[string]any) string {
	normalizedBase := trimTrailingSlash(baseURL)
	normalizedPath := path
	if normalizedPath != "" && !strings.HasPrefix(normalizedPath, "/") {
		normalizedPath = "/" + normalizedPath
	}
	if strings.HasSuffix(normalizedBase, "/v1") && strings.HasPrefix(normalizedPath, "/v1/") {
		normalizedPath = normalizedPath[3:]
	}
	if strings.HasSuffix(normalizedBase, "/v4") && strings.HasPrefix(normalizedPath, "/v4/") {
		normalizedPath = normalizedPath[3:]
	}
	withQuery := appendQueryFromMeta(normalizedPath, meta)
	return normalizedBase + withQuery
}

func appendQueryFromMeta(path string, meta map[string]any) string {
	if meta == nil {
		return path
	}
	raw := str(meta[metadata.CustomQueryParamsJSON])
	if raw == "" {
		return path
	}
	var qp map[string]any
	if err := json.Unmarshal([]byte(raw), &qp); err != nil || len(qp) == 0 {
		return path
	}
	sb := strings.Builder{}
	sb.WriteString(path)
	sep := "?"
	if strings.Contains(path, "?") {
		sep = "&"
	}
	for k, v := range qp {
		if strings.TrimSpace(k) == "" {
			continue
		}
		sb.WriteString(sep)
		sb.WriteString(url.QueryEscape(strings.TrimSpace(k)))
		sb.WriteString("=")
		sb.WriteString(url.QueryEscape(fmt.Sprint(v)))
		sep = "&"
	}
	return sb.String()
}

func applyHeaders(req *http.Request, def *catalog.Provider, apiKey string, meta map[string]any) {
	switch def.AuthMode {
	case catalog.AuthBearer:
		if apiKey != "" {
			req.Header.Set("Authorization", "Bearer "+apiKey)
		}
	case catalog.AuthXAPIKey:
		if apiKey != "" {
			req.Header.Set("x-api-key", apiKey)
			req.Header.Set("anthropic-version", "2023-06-01")
		}
	case catalog.AuthAPIKeyHeader:
		if apiKey != "" {
			req.Header.Set("api-key", apiKey)
		}
	case catalog.AuthToken:
		if apiKey != "" {
			req.Header.Set("Authorization", "Token "+apiKey)
		}
	}
	if meta == nil {
		return
	}
	raw := str(meta[metadata.CustomHeadersJSON])
	if raw == "" {
		return
	}
	var rows []map[string]any
	if err := json.Unmarshal([]byte(raw), &rows); err != nil {
		return
	}
	for _, row := range rows {
		if row == nil {
			continue
		}
		n := str(row["name"])
		if n == "" {
			continue
		}
		req.Header.Set(n, str(row["value"]))
	}
}

func extractErrJSON(body string) string {
	if strings.TrimSpace(body) == "" {
		return "模型服务返回异常"
	}
	var payload map[string]any
	if err := json.Unmarshal([]byte(body), &payload); err != nil {
		if len(body) > 200 {
			return body[:200]
		}
		return body
	}
	if errObj, ok := payload["error"].(map[string]any); ok {
		if m, ok := errObj["message"]; ok {
			return fmt.Sprint(m)
		}
	}
	if m, ok := payload["message"]; ok {
		return fmt.Sprint(m)
	}
	if len(body) > 200 {
		return body[:200]
	}
	return body
}
