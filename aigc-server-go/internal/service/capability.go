package service

import (
	"fmt"
	"sort"
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
	if isViduWorkspaceModel(mn) {
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
	if isViduWorkspaceModel(mn) {
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
	out := m.MergeCapabilities(tmp, resolved)
	return normalizeViduModelMetadata(out, provider, modelName)
}

func normalizeViduModelMetadata(meta map[string]any, provider, modelName string) map[string]any {
	if !strings.EqualFold(strings.TrimSpace(provider), "vidu") && !isViduWorkspaceModel(modelName) {
		return meta
	}
	family := detectViduModelFamily(modelName)
	if family == "" {
		family = "q2"
	}
	meta["viduFamily"] = family
	matrix := defaultViduMatrix(family)
	meta["viduDurations"] = ensureIntList(meta["viduDurations"], matrix.durations)
	meta["viduResolutions"] = ensureStringList(meta["viduResolutions"], matrix.resolutions)
	if _, ok := meta["viduAudioSupported"]; !ok {
		meta["viduAudioSupported"] = matrix.audioSupported
	}
	return meta
}

type viduMatrix struct {
	durations      []int
	resolutions    []string
	audioSupported bool
}

func defaultViduMatrix(family string) viduMatrix {
	switch strings.ToLower(strings.TrimSpace(family)) {
	case "q1":
		return viduMatrix{
			durations:      []int{4, 8},
			resolutions:    []string{"360p", "540p"},
			audioSupported: false,
		}
	case "q3":
		return viduMatrix{
			durations:      []int{4, 8},
			resolutions:    []string{"540p", "720p", "1080p"},
			audioSupported: true,
		}
	case "2.0":
		return viduMatrix{
			durations:      []int{4, 8},
			resolutions:    []string{"360p", "540p", "720p", "1080p"},
			audioSupported: true,
		}
	default:
		return viduMatrix{
			durations:      []int{4, 8},
			resolutions:    []string{"360p", "540p", "720p"},
			audioSupported: true,
		}
	}
}

func detectViduModelFamily(modelName string) string {
	n := strings.ToLower(strings.TrimSpace(modelName))
	switch {
	case strings.Contains(n, "q3"):
		return "q3"
	case strings.Contains(n, "q2"):
		return "q2"
	case strings.Contains(n, "q1"):
		return "q1"
	case strings.Contains(n, "2.0"), strings.Contains(n, "v2.0"), strings.Contains(n, "vidu2"):
		return "2.0"
	default:
		return ""
	}
}

func ensureIntList(raw any, defaults []int) []int {
	if vals := toIntList(raw); len(vals) > 0 {
		sort.Ints(vals)
		return vals
	}
	out := append([]int(nil), defaults...)
	sort.Ints(out)
	return out
}

func toIntList(raw any) []int {
	seen := map[int]struct{}{}
	var out []int
	switch v := raw.(type) {
	case []any:
		for _, x := range v {
			n := parseInt(fmt.Sprint(x))
			if n == nil {
				continue
			}
			if _, ok := seen[*n]; ok {
				continue
			}
			seen[*n] = struct{}{}
			out = append(out, *n)
		}
	case []int:
		for _, n := range v {
			if _, ok := seen[n]; ok {
				continue
			}
			seen[n] = struct{}{}
			out = append(out, n)
		}
	case string:
		for _, p := range strings.Split(v, ",") {
			n := parseInt(strings.TrimSpace(p))
			if n == nil {
				continue
			}
			if _, ok := seen[*n]; ok {
				continue
			}
			seen[*n] = struct{}{}
			out = append(out, *n)
		}
	}
	return out
}

func ensureStringList(raw any, defaults []string) []string {
	if vals := toStringList(raw); len(vals) > 0 {
		return vals
	}
	return append([]string(nil), defaults...)
}

func toStringList(raw any) []string {
	seen := map[string]struct{}{}
	var out []string
	switch v := raw.(type) {
	case []any:
		for _, x := range v {
			s := strings.TrimSpace(fmt.Sprint(x))
			if s == "" {
				continue
			}
			if _, ok := seen[s]; ok {
				continue
			}
			seen[s] = struct{}{}
			out = append(out, s)
		}
	case []string:
		for _, s := range v {
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
	case string:
		for _, p := range strings.Split(v, ",") {
			s := strings.TrimSpace(p)
			if s == "" {
				continue
			}
			if _, ok := seen[s]; ok {
				continue
			}
			seen[s] = struct{}{}
			out = append(out, s)
		}
	}
	return out
}

func parseInt(s string) *int {
	n := 0
	_, err := fmt.Sscanf(strings.TrimSpace(s), "%d", &n)
	if err != nil {
		return nil
	}
	return &n
}
