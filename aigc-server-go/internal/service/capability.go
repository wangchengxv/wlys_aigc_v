package service

import (
	"fmt"
	"strings"

	"github.com/example/aigc-server-go/internal/jsonutil"
	"github.com/example/aigc-server-go/internal/model"
)

type ModelCapability struct{}

func (ModelCapability) Supports(mc *model.ModelConfig, capability string) bool {
	if mc == nil || strings.TrimSpace(capability) == "" {
		return false
	}
	for _, c := range resolveCapabilities(mc) {
		if c == strings.ToLower(strings.TrimSpace(capability)) {
			return true
		}
	}
	return false
}

func resolveCapabilities(mc *model.ModelConfig) []string {
	meta := jsonutil.MapFromJSON(mc.MetadataJSON)
	var caps []string
	if raw, ok := meta["capabilities"]; ok {
		switch v := raw.(type) {
		case []any:
			for _, x := range v {
				s := strings.ToLower(strings.TrimSpace(fmt.Sprint(x)))
				if s != "" {
					caps = append(caps, s)
				}
			}
		case string:
			for _, p := range strings.Split(v, ",") {
				s := strings.ToLower(strings.TrimSpace(p))
				if s != "" {
					caps = append(caps, s)
				}
			}
		}
	}
	if len(caps) > 0 {
		return caps
	}
	mn := strings.ToLower(mc.ModelName)
	prov := strings.ToLower(mc.Provider)
	if strings.Contains(mn, "seedream") || strings.Contains(mn, "image") || strings.Contains(mn, "flux") ||
		strings.Contains(mn, "wanx") || strings.Contains(mn, "dall") || strings.Contains(mn, "sdxl") {
		return []string{"image"}
	}
	if strings.Contains(mn, "seedance") || strings.Contains(mn, "video") || strings.Contains(mn, "veo") || strings.Contains(mn, "sora") {
		return []string{"video"}
	}
	if prov != "ark" {
		return []string{"text"}
	}
	return []string{"image"}
}

// ResolveCapabilitiesFromMeta mirrors Java ModelCapabilityService.resolveCapabilities(metadata, provider, modelName).
func (ModelCapability) ResolveCapabilitiesFromMeta(meta map[string]any, provider, modelName string) []string {
	var caps []string
	if meta != nil {
		if raw, ok := meta["capabilities"]; ok {
			switch v := raw.(type) {
			case []any:
				for _, x := range v {
					s := strings.ToLower(strings.TrimSpace(fmt.Sprint(x)))
					if s != "" {
						caps = append(caps, s)
					}
				}
			case string:
				for _, p := range strings.Split(v, ",") {
					s := strings.ToLower(strings.TrimSpace(p))
					if s != "" {
						caps = append(caps, s)
					}
				}
			}
		}
	}
	if len(caps) > 0 {
		return caps
	}
	mn := strings.ToLower(modelName)
	prov := strings.ToLower(provider)
	if strings.Contains(mn, "seedream") || strings.Contains(mn, "image") || strings.Contains(mn, "flux") ||
		strings.Contains(mn, "wanx") || strings.Contains(mn, "dall") || strings.Contains(mn, "sdxl") {
		return []string{"image"}
	}
	if strings.Contains(mn, "seedance") || strings.Contains(mn, "video") || strings.Contains(mn, "veo") || strings.Contains(mn, "sora") {
		return []string{"video"}
	}
	if prov != "ark" {
		return []string{"text"}
	}
	return []string{"image"}
}

// MergeCapabilities sets normalized capabilities list on metadata (Java mergeCapabilities).
func (ModelCapability) MergeCapabilities(meta map[string]any, capabilities []string) map[string]any {
	merged := map[string]any{}
	for k, v := range meta {
		merged[k] = v
	}
	var norm []string
	seen := map[string]struct{}{}
	for _, c := range capabilities {
		v := strings.ToLower(strings.TrimSpace(c))
		if v == "" {
			continue
		}
		if _, ok := seen[v]; ok {
			continue
		}
		seen[v] = struct{}{}
		norm = append(norm, v)
	}
	merged["capabilities"] = norm
	return merged
}

// NormalizeModelMetadata merges inferred capabilities with metadata (Java ModelConfigService.normalizeMetadata).
func (m ModelCapability) NormalizeModelMetadata(meta map[string]any, provider, modelName string) map[string]any {
	src := meta
	if src == nil {
		src = map[string]any{}
	}
	tmp := map[string]any{}
	for k, v := range src {
		tmp[k] = v
	}
	resolved := m.ResolveCapabilitiesFromMeta(tmp, provider, modelName)
	return m.MergeCapabilities(tmp, resolved)
}
