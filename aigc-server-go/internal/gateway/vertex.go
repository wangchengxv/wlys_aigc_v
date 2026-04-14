package gateway

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/metadata"
	"golang.org/x/oauth2/google"
)

type VertexGateway struct {
	HTTP *http.Client
}

func (v *VertexGateway) GenerateContent(ctx context.Context, projectID, location, serviceAccountJSON string, openAiPayload map[string]any) (map[string]any, error) {
	if v.HTTP == nil {
		v.HTTP = &http.Client{Timeout: 120 * time.Second}
	}
	modelID := str(openAiPayload["model"])
	if modelID == "" {
		return nil, NewProviderError(400, "缺少 model")
	}
	userText := extractUserTextFromOpenAI(openAiPayload)
	if strings.TrimSpace(userText) == "" {
		return nil, NewProviderError(400, "缺少用户消息")
	}
	creds, err := google.CredentialsFromJSON(ctx, []byte(serviceAccountJSON), "https://www.googleapis.com/auth/cloud-platform")
	if err != nil {
		return nil, NewProviderError(400, "Service Account JSON 无效: "+err.Error())
	}
	tok, err := creds.TokenSource.Token()
	if err != nil {
		return nil, NewProviderError(400, "Vertex OAuth 失败: "+err.Error())
	}
	host := location + "-aiplatform.googleapis.com"
	path := fmt.Sprintf("/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent", projectID, location, modelID)
	body := map[string]any{
		"contents": []any{
			map[string]any{
				"role": "user",
				"parts": []any{
					map[string]any{"text": userText},
				},
			},
		},
	}
	raw, _ := json.Marshal(body)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, "https://"+host+path, bytes.NewReader(raw))
	if err != nil {
		return nil, NewProviderError(502, err.Error())
	}
	req.Header.Set("Authorization", "Bearer "+tok.AccessToken)
	req.Header.Set("Content-Type", "application/json")
	resp, err := v.HTTP.Do(req)
	if err != nil {
		return nil, NewProviderError(502, err.Error())
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 400 {
		return nil, NewProviderError(resp.StatusCode, string(b))
	}
	var root map[string]any
	if err := json.Unmarshal(b, &root); err != nil {
		return nil, NewProviderError(502, "Vertex 响应解析失败")
	}
	text := extractGeminiText(root)
	return openAiShapedMap(text), nil
}

func extractUserTextFromOpenAI(openAiPayload map[string]any) string {
	raw, ok := openAiPayload["messages"].([]any)
	if !ok {
		return ""
	}
	var sb strings.Builder
	for _, item := range raw {
		m, ok := item.(map[string]any)
		if !ok {
			continue
		}
		role := strings.ToLower(str(m["role"]))
		if role != "system" && role != "user" {
			continue
		}
		sb.WriteString(extractOpenAIContent(m["content"]))
	}
	return sb.String()
}

func extractGeminiText(root map[string]any) string {
	cands, _ := root["candidates"].([]any)
	if len(cands) == 0 {
		return ""
	}
	first, _ := cands[0].(map[string]any)
	content, _ := first["content"].(map[string]any)
	parts, _ := content["parts"].([]any)
	var sb strings.Builder
	for _, p := range parts {
		pm, ok := p.(map[string]any)
		if !ok {
			continue
		}
		if t := pm["text"]; t != nil {
			sb.WriteString(fmt.Sprint(t))
		}
	}
	return sb.String()
}

// openAiShapedMap wraps plain assistant text as an OpenAI-style chat completion JSON object.
func openAiShapedMap(text string) map[string]any {
	return map[string]any{
		"choices": []any{
			map[string]any{
				"index": 0,
				"message": map[string]any{
					"role":    "assistant",
					"content": text,
				},
				"finish_reason": "stop",
			},
		},
	}
}

// extractOpenAIContent parses OpenAI message content: string or multimodal parts array.
func extractOpenAIContent(content any) string {
	switch v := content.(type) {
	case string:
		return v
	case []any:
		var sb strings.Builder
		for _, part := range v {
			pm, ok := part.(map[string]any)
			if !ok {
				continue
			}
			if t := pm["text"]; t != nil {
				sb.WriteString(fmt.Sprint(t))
				continue
			}
			if strings.EqualFold(str(pm["type"]), "text") {
				sb.WriteString(str(pm["text"]))
			}
		}
		return sb.String()
	default:
		if content == nil {
			return ""
		}
		return fmt.Sprint(content)
	}
}

func (v *VertexGateway) DefaultGeminiModels() []string {
	_ = metadata.VertexProject
	return []string{"gemini-2.0-flash-001", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro"}
}
