package jsonutil

import (
	"encoding/json"
)

func StringSliceFromJSON(raw string) []string {
	if raw == "" {
		return nil
	}
	var out []string
	if err := json.Unmarshal([]byte(raw), &out); err != nil {
		return nil
	}
	return out
}

func ToJSONStringSlice(s []string) string {
	if s == nil {
		return "[]"
	}
	b, err := json.Marshal(s)
	if err != nil {
		return "[]"
	}
	return string(b)
}

func MapFromJSON(raw string) map[string]any {
	if raw == "" {
		return nil
	}
	var m map[string]any
	if err := json.Unmarshal([]byte(raw), &m); err != nil {
		return map[string]any{}
	}
	return m
}

func ToJSONMap(m map[string]any) string {
	if m == nil {
		return "{}"
	}
	b, err := json.Marshal(m)
	if err != nil {
		return "{}"
	}
	return string(b)
}
