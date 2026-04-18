package service

import (
	"strings"
	"testing"

	"github.com/example/aigc-server-go/internal/model"
)

func TestParseViduVideoURLs_SeparatesPrimaryAndWatermark(t *testing.T) {
	body := map[string]any{
		"creations": []any{
			map[string]any{
				"video_url":           "https://cdn.example.com/main.mp4",
				"watermark_video_url": "https://cdn.example.com/wm.mp4",
			},
		},
	}
	primary, watermark := parseViduVideoURLs(body)
	if primary != "https://cdn.example.com/main.mp4" {
		t.Fatalf("expected primary url, got %q", primary)
	}
	if watermark != "https://cdn.example.com/wm.mp4" {
		t.Fatalf("expected watermark url, got %q", watermark)
	}
}

func TestFlattenViduVideoURLs_PrimaryFirstAndDedup(t *testing.T) {
	body := map[string]any{
		"creations": []any{
			map[string]any{
				"url":                 "https://cdn.example.com/main.mp4",
				"watermark_video_url": "https://cdn.example.com/main.mp4",
			},
		},
	}
	got := flattenViduVideoURLs(body)
	if len(got) != 1 {
		t.Fatalf("expected deduped urls, got %+v", got)
	}
	if got[0] != "https://cdn.example.com/main.mp4" {
		t.Fatalf("unexpected first url: %q", got[0])
	}
}

func TestValidateAndNormalizeViduOptions_IsRecRemovesPromptAndMarksMeta(t *testing.T) {
	g := &Generation{}
	rm := &resolvedModel{
		Model: &model.ModelConfig{
			ModelName:    "viduq2-pro",
			MetadataJSON: `{"viduFamily":"q2"}`,
		},
	}
	payload := map[string]any{
		"prompt": "a cat",
		"is_rec": "true",
	}
	if err := g.validateAndNormalizeViduOptions(payload, rm); err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if _, ok := payload["prompt"]; ok {
		t.Fatalf("prompt should be removed when is_rec=true")
	}
	if payload["is_rec"] != true {
		t.Fatalf("is_rec should normalize to bool true")
	}
	meta := payload["meta_data"]
	if !strings.Contains(meta.(string), `"rec_mode_enabled":true`) {
		t.Fatalf("meta_data should contain rec_mode_enabled trace, got %v", meta)
	}
}

func TestValidateAndNormalizeViduOptions_AudioTypeRequiresAudio(t *testing.T) {
	g := &Generation{}
	rm := &resolvedModel{
		Model: &model.ModelConfig{
			ModelName:    "viduq3-pro",
			MetadataJSON: `{"viduFamily":"q3"}`,
		},
	}
	payload := map[string]any{
		"audio":      false,
		"audio_type": "all",
	}
	err := g.validateAndNormalizeViduOptions(payload, rm)
	if err == nil {
		t.Fatalf("expected validation error")
	}
	if !strings.Contains(err.Error(), "audio=false") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestParseArkVideoURL_NestedUnchanged(t *testing.T) {
	body := map[string]any{
		"data": map[string]any{
			"result": map[string]any{
				"video_url": "https://cdn.example.com/ark.mp4",
			},
		},
	}
	got := parseArkVideoURL(body, false)
	if got != "https://cdn.example.com/ark.mp4" {
		t.Fatalf("unexpected ark video url: %q", got)
	}
}

func TestIsViduWorkspaceModel_DoesNotIncludeKling(t *testing.T) {
	if isViduWorkspaceModel("kling-v1") {
		t.Fatalf("kling model must not be treated as vidu model")
	}
	if !isViduWorkspaceModel("viduq3-pro") {
		t.Fatalf("vidu model should be recognized")
	}
}
