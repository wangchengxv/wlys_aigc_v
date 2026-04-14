package service

import (
	"fmt"
	"strings"

	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/jsonutil"
	"github.com/example/aigc-server-go/internal/metadata"
)

var encryptedMetaKeys = map[string]struct{}{
	metadata.VertexSAJSON:          {},
	metadata.AWSSessionToken:       {},
	metadata.CustomHeadersJSON:     {},
	metadata.CustomQueryParamsJSON: {},
	metadata.ExtraAPIKeys:          {},
}

// NormalizeIncoming encrypts selected metadata values at rest (Java ConnectionMetadataHelper.normalizeIncoming).
func NormalizeIncoming(raw map[string]any, cr *crypto.Service) map[string]any {
	if len(raw) == 0 {
		return map[string]any{}
	}
	out := make(map[string]any)
	for k, v := range raw {
		if v == nil {
			continue
		}
		if _, enc := encryptedMetaKeys[k]; enc {
			plain := strings.TrimSpace(toString(v))
			if plain == "" {
				continue
			}
			if strings.HasPrefix(plain, "__ENC__") {
				out[k] = plain
			} else {
				enc, err := cr.Encrypt(plain)
				if err != nil {
					continue
				}
				out[k] = "__ENC__" + enc
			}
		} else {
			if s, ok := v.(string); ok {
				out[k] = strings.TrimSpace(s)
			} else {
				out[k] = v
			}
		}
	}
	return out
}

func decryptValue(stored string, cr *crypto.Service) string {
	if strings.TrimSpace(stored) == "" {
		return ""
	}
	if strings.HasPrefix(stored, "__ENC__") {
		p, err := cr.Decrypt(stored[7:])
		if err != nil {
			return ""
		}
		return p
	}
	return stored
}

// DecryptForUse decrypts encrypted metadata fields for gateway calls.
func DecryptForUse(stored map[string]any, cr *crypto.Service) map[string]any {
	if len(stored) == 0 {
		return map[string]any{}
	}
	out := make(map[string]any)
	for k, v := range stored {
		if _, enc := encryptedMetaKeys[k]; !enc {
			out[k] = v
			continue
		}
		s := ""
		if v != nil {
			s = strings.TrimSpace(toString(v))
		}
		out[k] = decryptValue(s, cr)
	}
	return out
}

// MaskForResponse masks encrypted keys for JSON responses (Java maskForResponse).
func MaskForResponse(stored map[string]any) map[string]any {
	if len(stored) == 0 {
		return map[string]any{}
	}
	out := make(map[string]any)
	for k, v := range stored {
		if _, enc := encryptedMetaKeys[k]; enc && v != nil && strings.TrimSpace(toString(v)) != "" {
			out[k] = "********"
		} else {
			out[k] = v
		}
	}
	return out
}

// MergeMetadata patches metadata on update (Java ConnectionMetadataHelper.merge).
func MergeMetadata(existingJSON string, patch map[string]any, cr *crypto.Service) map[string]any {
	base := jsonutil.MapFromJSON(existingJSON)
	if base == nil {
		base = map[string]any{}
	}
	if patch == nil {
		return base
	}
	for k, v := range patch {
		if v == nil {
			delete(base, k)
			continue
		}
		if _, enc := encryptedMetaKeys[k]; enc {
			plain := strings.TrimSpace(toString(v))
			if plain == "" || plain == "********" {
				continue
			}
			if strings.HasPrefix(plain, "__ENC__") {
				base[k] = plain
			} else {
				enc, err := cr.Encrypt(plain)
				if err != nil {
					continue
				}
				base[k] = "__ENC__" + enc
			}
		} else {
			base[k] = v
		}
	}
	return base
}

func toString(v any) string {
	if v == nil {
		return ""
	}
	return strings.TrimSpace(fmt.Sprint(v))
}
