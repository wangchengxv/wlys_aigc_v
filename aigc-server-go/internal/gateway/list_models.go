package gateway

import (
	"context"
	"net/url"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/metadata"
)

// ListModels mirrors ProviderHttpGateway.listModels (subset: HTTP/Azure/Ollama/Vertex/Moark/Bedrock stub).
func (g *HTTPGateway) ListModels(ctx context.Context, def *catalog.Provider, baseURL, apiKey string, meta map[string]any, timeout time.Duration) ([]string, error) {
	if meta == nil {
		meta = map[string]any{}
	}
	static := def.StaticModels
	oneLink := strings.EqualFold(def.Key, "onelinkai")

	switch def.Kind {
	case catalog.KindBedrock:
		region := str(meta[metadata.AWSRegion])
		ak := str(meta[metadata.AWSAccessKeyID])
		if region == "" || ak == "" || strings.TrimSpace(apiKey) == "" {
			return []string{}, nil
		}
		return []string{}, nil
	case catalog.KindMoarkI2V:
		if len(static) == 0 {
			return []string{}, nil
		}
		return append([]string(nil), static...), nil
	case catalog.KindVertex:
		return []string{
			"gemini-2.0-flash-001",
			"gemini-1.5-flash",
			"gemini-1.5-pro",
			"gemini-1.0-pro",
		}, nil
	case catalog.KindAzure:
		ver := str(meta[metadata.AzureAPIVersion])
		if ver == "" {
			ver = metadata.AzureDefaultAPIVersion
		}
		path := "/openai/models?api-version=" + url.QueryEscape(ver)
		ctx2, cancel := context.WithTimeout(ctx, timeout)
		defer cancel()
		resp, err := g.GetJSON(ctx2, baseURL, path, def, apiKey, meta, timeout)
		if err != nil {
			return nil, err
		}
		return parseOpenAIModelIDs(resp), nil
	}

	if !oneLink && len(static) > 0 {
		return append([]string(nil), static...), nil
	}
	if def.ModelsPath == "" {
		out := append([]string(nil), static...)
		return out, nil
	}

	ctx2, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()
	resp, err := g.GetJSON(ctx2, baseURL, def.ModelsPath, def, apiKey, meta, timeout)
	if err != nil {
		if oneLink && len(static) > 0 {
			return append([]string(nil), static...), nil
		}
		return nil, err
	}

	var remote []string
	if strings.EqualFold(def.Key, "ollama") {
		remote = parseOllamaModelNames(resp)
	} else {
		remote = parseOpenAIModelIDs(resp)
	}
	if oneLink {
		return mergeUnique(static, remote), nil
	}
	return remote, nil
}

func parseOpenAIModelIDs(resp map[string]any) []string {
	data, _ := resp["data"].([]any)
	var out []string
	for _, item := range data {
		m, ok := item.(map[string]any)
		if !ok {
			continue
		}
		if id := str(m["id"]); id != "" {
			out = append(out, id)
		}
	}
	return out
}

func parseOllamaModelNames(resp map[string]any) []string {
	models, _ := resp["models"].([]any)
	var out []string
	for _, item := range models {
		m, ok := item.(map[string]any)
		if !ok {
			continue
		}
		if n := str(m["name"]); n != "" {
			out = append(out, n)
		}
	}
	return out
}

func mergeUnique(a, b []string) []string {
	seen := map[string]struct{}{}
	var out []string
	for _, s := range a {
		s = strings.TrimSpace(s)
		if s == "" {
			continue
		}
		if _, ok := seen[s]; ok {
			continue
		}
		seen[s] = struct{}{}
		out = append(out, s)
	}
	for _, s := range b {
		s = strings.TrimSpace(s)
		if s == "" {
			continue
		}
		if _, ok := seen[s]; ok {
			continue
		}
		seen[s] = struct{}{}
		out = append(out, s)
	}
	return out
}
