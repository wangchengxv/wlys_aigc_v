package service

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/config"
	"github.com/example/aigc-server-go/internal/gateway"
	"github.com/example/aigc-server-go/internal/model"
)

func TestParseViduVideoURLs_SeparatesPrimaryAndWatermark(t *testing.T) {
	body := map[string]any{
		"creations": []any{
			map[string]any{
				"video_url":           "https://api.onelinkai.cloud/main.mp4",
				"watermark_video_url": "https://api.onelinkai.cloud/wm.mp4",
			},
		},
	}
	primary, watermark := parseViduVideoURLs(body)
	if primary != "https://api.onelinkai.cloud/main.mp4" {
		t.Fatalf("expected primary url, got %q", primary)
	}
	if watermark != "https://api.onelinkai.cloud/wm.mp4" {
		t.Fatalf("expected watermark url, got %q", watermark)
	}
}

func TestFlattenViduVideoURLs_PrimaryFirstAndDedup(t *testing.T) {
	body := map[string]any{
		"creations": []any{
			map[string]any{
				"url":                 "https://api.onelinkai.cloud/main.mp4",
				"watermark_video_url": "https://api.onelinkai.cloud/main.mp4",
			},
		},
	}
	got := flattenViduVideoURLs(body)
	if len(got) != 1 {
		t.Fatalf("expected deduped urls, got %+v", got)
	}
	if got[0] != "https://api.onelinkai.cloud/main.mp4" {
		t.Fatalf("unexpected first url: %q", got[0])
	}
}

func TestValidateAndNormalizeViduOptions_IsRecRemovesPromptAndMarksMeta(t *testing.T) {
	g := &Generation{}
	rm := &resolvedModel{
		Model: &model.ModelConfig{
			ModelName:    "image-vidu-q2",
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
			ModelName:    "video-viduq3-pro",
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
				"video_url": "https://api.onelinkai.cloud/ark.mp4",
			},
		},
	}
	got := parseArkVideoURL(body, false)
	if got != "https://api.onelinkai.cloud/ark.mp4" {
		t.Fatalf("unexpected ark video url: %q", got)
	}
}

func TestIsViduWorkspaceModel_DoesNotIncludeKling(t *testing.T) {
	if isViduWorkspaceModel("kling-v1") {
		t.Fatalf("kling model must not be treated as vidu model")
	}
	if !isViduWorkspaceModel("video-viduq3-pro") {
		t.Fatalf("vidu model should be recognized")
	}
}

func TestValidateOneLinkDoubaoReferenceImage_RequiresHTTPURL(t *testing.T) {
	if _, err := validateOneLinkDoubaoReferenceImage(""); err == nil {
		t.Fatalf("expected missing image error")
	}
	if _, err := validateOneLinkDoubaoReferenceImage("data:image/png;base64,xxx"); err == nil {
		t.Fatalf("expected non-http image error")
	}
	ref, err := validateOneLinkDoubaoReferenceImage(" https://api.onelinkai.cloud/ref.png ")
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if ref != "https://api.onelinkai.cloud/ref.png" {
		t.Fatalf("unexpected ref: %q", ref)
	}
}

func TestCallOneLinkDoubaoVideo_UsesVolcPathAndContentShape(t *testing.T) {
	var captured map[string]any
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case r.Method == http.MethodPost && r.URL.Path == "/volc/api/v3/contents/generations/tasks":
			if got := r.Header.Get("Authorization"); got != "Bearer secret-key" {
				t.Fatalf("unexpected auth header: %q", got)
			}
			defer r.Body.Close()
			if err := json.NewDecoder(r.Body).Decode(&captured); err != nil {
				t.Fatalf("decode payload failed: %v", err)
			}
			_, _ = w.Write([]byte(`{"task_id":"task-1"}`))
		case r.Method == http.MethodGet && r.URL.Path == "/volc/api/v3/contents/generations/tasks/task-1":
			_, _ = w.Write([]byte(`{"data":{"video_url":"https://api.onelinkai.cloud/out.mp4"}}`))
		default:
			t.Fatalf("unexpected request: %s %s", r.Method, r.URL.Path)
		}
	}))
	defer srv.Close()

	g := &Generation{
		Cfg: &config.Config{
			ArkVideoDurationSeconds: 5,
			ArkWatermark:            true,
			ArkVideoPollMaxAttempts: 5,
			ArkVideoPollIntervalMs:  300,
		},
		GW: &gateway.HTTPGateway{HTTP: srv.Client()},
	}
	def := &catalog.Provider{
		Key:      "onelinkai",
		AuthMode: catalog.AuthBearer,
		Kind:     catalog.KindOpenAICompat,
	}
	got, err := g.callOneLinkDoubaoVideo(def, srv.URL, "secret-key", nil, "无人机高速穿越峡谷", "https://api.onelinkai.cloud/ref.png")
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if got != "https://api.onelinkai.cloud/out.mp4" {
		t.Fatalf("unexpected video url: %q", got)
	}
	if captured["model"] != "doubao-seedance-1.5-pro" {
		t.Fatalf("unexpected model: %v", captured["model"])
	}
	content, ok := captured["content"].([]any)
	if !ok || len(content) != 2 {
		t.Fatalf("unexpected content node: %#v", captured["content"])
	}
	textNode, ok := content[0].(map[string]any)
	if !ok || textNode["type"] != "text" {
		t.Fatalf("unexpected first content node: %#v", content[0])
	}
	imageNode, ok := content[1].(map[string]any)
	if !ok || imageNode["type"] != "image_url" {
		t.Fatalf("unexpected second content node: %#v", content[1])
	}
	imageWrap, ok := imageNode["image_url"].(map[string]any)
	if !ok || imageWrap["url"] != "https://api.onelinkai.cloud/ref.png" {
		t.Fatalf("unexpected image_url node: %#v", imageNode["image_url"])
	}
}
