package service

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"image"
	_ "image/gif"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/config"
	"github.com/example/aigc-server-go/internal/consts"
	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/errs"
	"github.com/example/aigc-server-go/internal/gateway"
	"github.com/example/aigc-server-go/internal/jsonutil"
	"github.com/example/aigc-server-go/internal/model"
	"github.com/example/aigc-server-go/internal/repo"
)

// viduOneLinkVideoProvider keeps backward compatibility for historical OneLink + Vidu configs.
var viduOneLinkVideoProvider = &catalog.Provider{
	Key:             "vidu_onelink",
	AuthMode:        catalog.AuthBearer,
	VideoSubmitPath: "/vidu/ent/v2/img2video",
	VideoResultPath: "/vidu/ent/v2/tasks/{taskId}/creations",
	Kind:            catalog.KindOpenAICompat,
}

// klingOneLinkVideoProvider for Kling text-to-video and image-to-video via OneLink.
var klingOneLinkVideoProvider = &catalog.Provider{
	Key:      "kling",
	AuthMode: catalog.AuthBearer,
	Kind:     catalog.KindOpenAICompat,
}

// klingImageToVideoPath returns the Kling image-to-video submit path.
func klingImageToVideoPath() string {
	return "/kling/v1/videos/image2video"
}

// klingTextToVideoPath returns the Kling text-to-video submit path.
func klingTextToVideoPath() string {
	return "/kling/v1/videos/text2video"
}

// klingResultPath returns the Kling task result query path (both T2V and I2V use the same task endpoint).
func klingResultPath() string {
	return "/kling/v1/videos/tasks/{taskId}"
}

type Generation struct {
	Store   *repo.Stores
	Script  *ScriptProject
	Cfg     *config.Config
	Cat     *catalog.Catalog
	Crypto  *crypto.Service
	GW      *gateway.HTTPGateway
	Local   *LocalFile
	Routing *RouterRouting
	Cap     *ModelCapability
	VS      VideoStyle
}

const maxVideoMergedPromptChars = 8000

var blockWords = []string{"暴恐", "色情", "违禁", "涉政"}

func (g *Generation) checkPrompt(prompt string) error {
	for _, w := range blockWords {
		if strings.Contains(prompt, w) {
			return errs.New(400, "输入内容包含敏感词，请调整后重试")
		}
	}
	return nil
}

func (g *Generation) Generate(owner string, req map[string]any) (map[string]any, error) {
	prompt := strVal(req["prompt"])
	if err := g.checkPrompt(prompt); err != nil {
		return nil, err
	}
	style := strVal(req["style"])
	if err := g.checkPrompt(style); err != nil {
		return nil, err
	}
	mode := strVal(req["mode"])
	if owner == "" {
		return nil, errs.New(401, "缺少用户标识")
	}
	if mode != "text" && mode != "image" && mode != "video" && mode != "both" {
		return nil, errs.New(400, "不支持的模式")
	}
	start := time.Now()
	taskID := "T" + randomHex(16)

	task := &model.GenerationTask{
		TaskID:    taskID,
		OwnerID:   owner,
		Prompt:    prompt,
		Mode:      mode,
		Style:     style,
		CreatedAt: time.Now(),
		Status:    "PROCESSING",
	}
	_ = g.Store.SaveTask(task)

	var textRes, imgRes, vidRes []string
	var err error
	defer func() {
		task.LatencyMs = time.Since(start).Milliseconds()
		task.TextResultsJSON = jsonutil.ToJSONStringSlice(textRes)
		task.ImageResultsJSON = jsonutil.ToJSONStringSlice(imgRes)
		task.VideoResultsJSON = jsonutil.ToJSONStringSlice(vidRes)
		if err != nil {
			if biz, ok := err.(*errs.Biz); ok {
				task.Status = "FAIL"
				task.ErrorCode = fmt.Sprintf("BIZ_%d", biz.Status)
			} else {
				task.Status = "FAIL"
				task.ErrorCode = "INTERNAL_ERROR"
			}
		} else {
			task.Status = "SUCCESS"
			task.ErrorCode = ""
		}
		_ = g.Store.SaveTask(task)
	}()

	if mode == "text" || mode == "both" {
		textRes, err = g.genText(prompt, style, strVal(req["textLength"]))
		if err != nil {
			return nil, err
		}
	}
	if mode == "image" || mode == "both" {
		cnt := safeCount(req["count"])
		imgRes, task.ImageModel, task.ImageModelSource, task.ImageModelMatchedBy, task.ImageModelRejectReason, err = g.genImages(prompt, cnt, strVal(req["imageModel"]))
		if err != nil {
			return nil, err
		}
	}
	if mode == "video" {
		cnt := safeCount(req["count"])
		vp := g.buildVideoPrompt(style, prompt)
		if len(vp) > maxVideoMergedPromptChars {
			return nil, errs.New(400, "视频提示词过长（含全局风格与用户描述），请缩短后重试")
		}
		vidRes, task.VideoModel, task.VideoModelSource, task.VideoModelMatchedBy, task.VideoModelRejectReason, err = g.genVideos(vp, cnt, strVal(req["videoModel"]), strVal(req["videoReferenceImageUrl"]), extractVideoViduOptions(req))
		if err != nil {
			return nil, err
		}
	}

	g.persistWorkspace(taskID, task, imgRes, vidRes)
	return g.taskToMap(task), nil
}

func strVal(v any) string {
	if v == nil {
		return ""
	}
	return strings.TrimSpace(fmt.Sprint(v))
}

func safeCount(v any) int {
	n := 1
	switch t := v.(type) {
	case float64:
		n = int(t)
	case int:
		n = t
	}
	if n < 1 {
		n = 1
	}
	if n > 4 {
		n = 4
	}
	return n
}

func (g *Generation) buildVideoPrompt(styleKey, prompt string) string {
	anchor := g.VS.AnchorByStyleKey(styleKey)
	neg := g.VS.VideoNegativeEnglish()
	s := "Visual Style Anchor: " + anchor
	p := strings.TrimSpace(prompt)
	n := "Negative Prompt: " + neg
	if s == "" {
		return p
	}
	if p == "" {
		return s + "\n" + n
	}
	return s + "\n" + p + "\n" + n
}

func (g *Generation) genText(prompt, style, textLength string) ([]string, error) {
	rm := g.resolveModel("text", "")
	if rm == nil {
		return nil, errs.New(400, "未配置可用的文本模型")
	}
	payload := g.buildTextRequest(prompt, style, textLength, rm)
	ctx := context.Background()
	resp, err := g.GW.InvokeChat(ctx, rm.Provider, rm.Conn.BaseURL, rm.APIKey, rm.Meta, payload, time.Duration(g.Routing.TimeoutSeconds())*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return nil, errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
		}
		return nil, err
	}
	content := parseAssistantContent(resp, rm.Provider.APIFormat)
	lines := splitTextResults(content)
	if len(lines) == 0 {
		return nil, errs.New(502, "文本模型返回为空")
	}
	return lines, nil
}

type resolvedModel struct {
	Model    *model.ModelConfig
	Conn     *model.ConnectionConfig
	Provider *catalog.Provider
	APIKey   string
	Meta     map[string]any
	Source   string
	Matched  string
	Reject   string
}

func (g *Generation) resolveModel(capability, requested string) *resolvedModel {
	models, _ := g.Store.AllModels()
	var cand []*model.ModelConfig
	for i := range models {
		m := &models[i]
		if !m.Enabled {
			continue
		}
		if !g.Cap.Supports(m, capability) {
			continue
		}
		cand = append(cand, m)
	}
	if len(cand) == 0 {
		return nil
	}
	if strings.TrimSpace(requested) != "" {
		for _, m := range cand {
			if by := g.matchExplicitModel(m, requested); by != "" {
				if r := g.toResolved(m); r != nil {
					r.Matched = by
					return r
				}
			}
		}
		return nil
	}
	ordered := g.Routing.OrderedConnectionIDs(true)
	for _, cid := range ordered {
		for _, m := range cand {
			if m.ConnectionID == cid {
				if r := g.toResolved(m); r != nil {
					return r
				}
			}
		}
	}
	for _, m := range cand {
		if r := g.toResolved(m); r != nil {
			return r
		}
	}
	return nil
}

func (g *Generation) toResolved(m *model.ModelConfig) *resolvedModel {
	c, err := g.Store.GetConnection(m.ConnectionID)
	if err != nil || !c.Enabled {
		return nil
	}
	p, err := g.Cat.Require(c.Provider)
	if err != nil {
		return nil
	}
	meta := jsonutil.MapFromJSON(c.MetadataJSON)
	apiKey := g.resolveAPIKey(p, c, meta)
	return &resolvedModel{Model: m, Conn: c, Provider: p, APIKey: apiKey, Meta: meta, Source: "USER_CONFIGURED"}
}

func (g *Generation) resolveAPIKey(p *catalog.Provider, c *model.ConnectionConfig, meta map[string]any) string {
	switch p.Kind {
	case catalog.KindBedrock:
		if c.EncryptedAPIKey == "" {
			return ""
		}
		k, _ := g.Crypto.Decrypt(c.EncryptedAPIKey)
		return k
	case catalog.KindVertex:
		return ""
	}
	if p.AuthMode == catalog.AuthNone {
		return ""
	}
	if c.EncryptedAPIKey == "" {
		return ""
	}
	k, _ := g.Crypto.Decrypt(c.EncryptedAPIKey)
	return k
}

func (g *Generation) matchExplicitModel(mc *model.ModelConfig, requested string) string {
	exp := normalizeModel(requested)
	if exp == "" {
		return ""
	}
	if exp == normalizeModel(mc.ModelName) {
		return "modelName"
	}
	if exp == normalizeModel(mc.Name) {
		return "name"
	}
	meta := jsonutil.MapFromJSON(mc.MetadataJSON)
	if raw, ok := meta["aliases"]; ok {
		switch a := raw.(type) {
		case []any:
			for _, x := range a {
				if exp == normalizeModel(fmt.Sprint(x)) {
					return "alias"
				}
			}
		case string:
			for _, p := range strings.Split(a, ",") {
				if exp == normalizeModel(p) {
					return "alias"
				}
			}
		}
	}
	return ""
}

func normalizeModel(s string) string {
	return strings.ToLower(strings.TrimSpace(s))
}

func (g *Generation) buildTextRequest(prompt, style, textLength string, rm *resolvedModel) map[string]any {
	lengthDesc := "中等长度，适合朋友圈、公众号摘要或落地页"
	switch textLength {
	case "short":
		lengthDesc = "短一点，适合海报或 banner"
	case "long":
		lengthDesc = "长一点，适合详情页或社媒长文案"
	}
	systemPrompt := "你是一名中文营销创意助手，擅长产出可直接使用的高质量文案。"
	userPrompt := fmt.Sprintf(`请围绕以下主题生成 2 条中文营销文案，要求：
1. 风格为：%s
2. 长度要求：%s
3. 每条文案都要完整、可直接发布
4. 请不要输出解释，只输出两条文案，每条单独一行

主题：%s`, style, lengthDesc, prompt)
	if strings.EqualFold(rm.Provider.APIFormat, "anthropic") {
		return map[string]any{
			"model":       rm.Model.ModelName,
			"system":      systemPrompt,
			"messages":    []any{map[string]any{"role": "user", "content": userPrompt}},
			"max_tokens":  1000,
			"temperature": 0.8,
		}
	}
	return map[string]any{
		"model": rm.Model.ModelName,
		"messages": []any{
			map[string]any{"role": "system", "content": systemPrompt},
			map[string]any{"role": "user", "content": userPrompt},
		},
		"temperature": 0.8,
		"max_tokens":  1000,
	}
}

func parseAssistantContent(resp map[string]any, apiFormat string) string {
	if strings.EqualFold(apiFormat, "anthropic") {
		if c, ok := resp["content"].([]any); ok && len(c) > 0 {
			if first, ok := c[0].(map[string]any); ok {
				return strVal(first["text"])
			}
		}
		return ""
	}
	if choices, ok := resp["choices"].([]any); ok && len(choices) > 0 {
		if first, ok := choices[0].(map[string]any); ok {
			if msg, ok := first["message"].(map[string]any); ok {
				return strVal(msg["content"])
			}
		}
	}
	return ""
}

func splitTextResults(content string) []string {
	if strings.TrimSpace(content) == "" {
		return nil
	}
	lines := strings.Split(content, "\n")
	var out []string
	bullet := regexp.MustCompile(`^[\-•\d\.\)]\s*`)
	for _, line := range lines {
		line = strings.TrimSpace(line)
		line = bullet.ReplaceAllString(line, "")
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		out = append(out, line)
		if len(out) >= 4 {
			break
		}
	}
	if len(out) == 0 {
		return []string{strings.TrimSpace(content)}
	}
	// distinct
	seen := map[string]struct{}{}
	var u []string
	for _, l := range out {
		if _, ok := seen[l]; ok {
			continue
		}
		seen[l] = struct{}{}
		u = append(u, l)
	}
	return u
}

func mapProviderStatus(code int) int {
	if code == 401 || code == 403 {
		return 502
	}
	if code == 408 || code == 504 {
		return 504
	}
	return 502
}

func (g *Generation) genImages(prompt string, count int, requested string) ([]string, string, string, string, string, error) {
	rm := g.resolveModel("image", requested)
	if rm != nil {
		var imgs []string
		for i := 0; i < count; i++ {
			var body map[string]any
			var err error
			if strings.EqualFold(rm.Provider.Key, "ark") {
				body, err = g.callArkImage(rm.Conn.BaseURL, rm.APIKey, prompt, rm.Model.ModelName)
			} else {
				body, err = g.GW.GenerateImage(context.Background(), rm.Provider, rm.Conn.BaseURL, rm.APIKey, map[string]any{
					"model": rm.Model.ModelName, "prompt": prompt, "n": 1, "size": "1024x1024",
					"response_format": "url",
				}, 60*time.Second)
			}
			if err != nil {
				if pe, ok := err.(*gateway.ProviderError); ok {
					return nil, "", "", "", "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
				}
				return nil, "", "", "", "", err
			}
			u := parseImageURL(body)
			if u == "" {
				return nil, "", "", "", "", errs.New(502, "模型服务返回异常，未获取到图片地址")
			}
			imgs = append(imgs, u)
		}
		return imgs, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
	}
	if strings.TrimSpace(requested) != "" {
		return nil, "", "", "", "", errs.New(400, "图片模型未在可用配置中")
	}
	modelName := g.Cfg.ArkDefaultImageModel
	var imgs []string
	for i := 0; i < count; i++ {
		body, err := g.callArkImage(g.Cfg.ArkBaseURL, g.Cfg.ArkAPIKey, prompt, modelName)
		if err != nil {
			if pe, ok := err.(*gateway.ProviderError); ok {
				return nil, "", "", "", "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
			}
			return nil, "", "", "", "", err
		}
		u := parseImageURL(body)
		if u == "" {
			return nil, "", "", "", "", errs.New(502, "模型服务返回异常，未获取到图片地址")
		}
		imgs = append(imgs, u)
	}
	return imgs, modelName, "SYSTEM_FALLBACK", "default-image", "", nil
}

func (g *Generation) callArkImage(baseURL, apiKey, prompt, imageModel string) (map[string]any, error) {
	p, _ := g.Cat.Require("ark")
	payload := map[string]any{
		"model":                       imageModel,
		"prompt":                      prompt,
		"sequential_image_generation": g.Cfg.ArkSequentialImageGen,
		"response_format":             g.Cfg.ArkResponseFormat,
		"size":                        g.Cfg.ArkSize,
		"stream":                      g.Cfg.ArkStream,
		"watermark":                   g.Cfg.ArkWatermark,
	}
	return g.GW.GenerateImage(context.Background(), p, baseURL, apiKey, payload, 60*time.Second)
}

func parseImageURL(body map[string]any) string {
	d, ok := body["data"].([]any)
	if !ok || len(d) == 0 {
		return ""
	}
	first, ok := d[0].(map[string]any)
	if !ok {
		return ""
	}
	if u := first["url"]; u != nil {
		return fmt.Sprint(u)
	}
	if b := first["b64_json"]; b != nil {
		return fmt.Sprint(b)
	}
	return ""
}

// viduImg2VideoAllowedKeys matches Vidu img2video optional keys (excluding model/images/prompt).
var viduImg2VideoAllowedKeys = map[string]struct{}{
	"duration": {}, "seed": {}, "resolution": {}, "movement_amplitude": {},
	"payload": {}, "off_peak": {}, "watermark": {}, "wm_position": {},
	"wm_url": {}, "meta_data": {}, "callback_url": {},
	"audio": {}, "audio_type": {}, "voice_id": {}, "is_rec": {}, "bgm": {},
}

const (
	viduMaxDataImageBytes  = 10 * 1024 * 1024
	viduMaxRemoteImageSize = 10 * 1024 * 1024
)

var viduAudioTypeAllowed = map[string]struct{}{
	"all": {}, "speech_only": {}, "sound_effect_only": {},
}

func extractVideoViduOptions(req map[string]any) map[string]any {
	raw, ok := req["videoViduOptions"]
	if !ok || raw == nil {
		return nil
	}
	var m map[string]any
	switch v := raw.(type) {
	case map[string]any:
		m = v
	case string:
		s := strings.TrimSpace(v)
		if s == "" {
			return nil
		}
		if err := json.Unmarshal([]byte(s), &m); err != nil {
			return nil
		}
	default:
		b, err := json.Marshal(raw)
		if err != nil {
			return nil
		}
		if err := json.Unmarshal(b, &m); err != nil {
			return nil
		}
	}
	out := make(map[string]any)
	for k, val := range m {
		k = strings.TrimSpace(k)
		if _, allowed := viduImg2VideoAllowedKeys[k]; !allowed {
			continue
		}
		if val == nil {
			continue
		}
		if s, ok := val.(string); ok && strings.TrimSpace(s) == "" {
			continue
		}
		out[k] = val
	}
	if len(out) == 0 {
		return nil
	}
	return out
}

func mergeViduImg2VideoPayload(base map[string]any, extra map[string]any) {
	if extra == nil {
		return
	}
	for k, v := range extra {
		if k == "model" || k == "images" || k == "prompt" {
			continue
		}
		base[k] = v
	}
}

func (g *Generation) buildViduPayload(rm *resolvedModel, prompt, imageRef string, extras map[string]any) (map[string]any, error) {
	if err := g.validateViduImage(imageRef); err != nil {
		return nil, err
	}
	payload := map[string]any{
		"model":  rm.Model.ModelName,
		"images": []any{imageRef},
	}
	if strings.TrimSpace(prompt) != "" {
		payload["prompt"] = strings.TrimSpace(prompt)
	}
	mergeViduImg2VideoPayload(payload, extras)
	if err := g.validateAndNormalizeViduOptions(payload, rm); err != nil {
		return nil, err
	}
	return payload, nil
}

func (g *Generation) validateAndNormalizeViduOptions(payload map[string]any, rm *resolvedModel) error {
	modelMeta := jsonutil.MapFromJSON(rm.Model.MetadataJSON)
	modelName := rm.Model.ModelName
	family := firstNonBlank(strVal(modelMeta["viduFamily"]), detectViduModelFamily(modelName), "q2")
	matrix := defaultViduMatrix(family)
	allowedDurations := ensureIntList(modelMeta["viduDurations"], matrix.durations)
	allowedResolutions := ensureStringList(modelMeta["viduResolutions"], matrix.resolutions)
	audioSupported := matrix.audioSupported
	if v, ok := modelMeta["viduAudioSupported"].(bool); ok {
		audioSupported = v
	}

	if raw := payload["duration"]; raw != nil {
		d, ok := parseViduInt(raw)
		if !ok {
			return errs.New(400, "Vidu 参数 duration 必须为整数秒")
		}
		if !containsInt(allowedDurations, d) {
			return errs.New(400, fmt.Sprintf("Vidu 模型族 %s 不支持 duration=%d，可选：%s", family, d, joinInts(allowedDurations)))
		}
		payload["duration"] = d
	}
	if raw := payload["resolution"]; raw != nil {
		res := strings.TrimSpace(fmt.Sprint(raw))
		if res != "" && !containsFold(allowedResolutions, res) {
			return errs.New(400, fmt.Sprintf("Vidu 模型族 %s 不支持 resolution=%s，可选：%s", family, res, strings.Join(allowedResolutions, ", ")))
		}
		payload["resolution"] = res
	}
	audioEnabled := false
	if raw, ok := payload["audio"]; ok {
		b, ok := parseViduBool(raw)
		if !ok {
			return errs.New(400, "Vidu 参数 audio 必须为布尔值")
		}
		payload["audio"] = b
		audioEnabled = b
	}
	if !audioSupported && audioEnabled {
		return errs.New(400, fmt.Sprintf("Vidu 模型族 %s 不支持 audio=true", family))
	}
	if raw, ok := payload["audio_type"]; ok {
		audioType := strings.ToLower(strings.TrimSpace(fmt.Sprint(raw)))
		if audioType != "" {
			if _, allowed := viduAudioTypeAllowed[audioType]; !allowed {
				return errs.New(400, "Vidu 参数 audio_type 仅支持 all/speech_only/sound_effect_only")
			}
			if !audioEnabled {
				return errs.New(400, "audio=false 时不允许传 audio_type")
			}
			payload["audio_type"] = audioType
		}
	}
	if raw, ok := payload["voice_id"]; ok {
		voiceID := strings.TrimSpace(fmt.Sprint(raw))
		if voiceID != "" && !audioEnabled {
			return errs.New(400, "audio=false 时不允许传 voice_id")
		}
		payload["voice_id"] = voiceID
	}
	if raw, ok := payload["bgm"]; ok {
		bgm, ok := parseViduBool(raw)
		if !ok {
			return errs.New(400, "Vidu 参数 bgm 必须为布尔值")
		}
		if bgm && !audioEnabled {
			return errs.New(400, "audio=false 时不允许开启 bgm")
		}
		payload["bgm"] = bgm
	}
	if raw, ok := payload["wm_position"]; ok {
		pos, ok := parseViduInt(raw)
		if !ok || pos < 1 || pos > 4 {
			return errs.New(400, "Vidu 参数 wm_position 必须在 1-4 之间")
		}
		payload["wm_position"] = pos
	}
	if raw, ok := payload["is_rec"]; ok {
		isRec, ok := parseViduBool(raw)
		if !ok {
			return errs.New(400, "Vidu 参数 is_rec 必须为布尔值")
		}
		payload["is_rec"] = isRec
		if isRec {
			delete(payload, "prompt")
			// Keep recommendation mode traceable via meta_data for later inspection.
			payload["meta_data"] = mergeViduMetaData(payload["meta_data"], map[string]any{"rec_mode_enabled": true})
		}
	}
	return nil
}

func mergeViduMetaData(raw any, patch map[string]any) string {
	out := map[string]any{}
	if raw != nil {
		switch v := raw.(type) {
		case map[string]any:
			for k, val := range v {
				out[k] = val
			}
		case string:
			var parsed map[string]any
			if err := json.Unmarshal([]byte(strings.TrimSpace(v)), &parsed); err == nil {
				for k, val := range parsed {
					out[k] = val
				}
			}
		}
	}
	for k, val := range patch {
		out[k] = val
	}
	b, _ := json.Marshal(out)
	return string(b)
}

func parseViduInt(raw any) (int, bool) {
	switch v := raw.(type) {
	case int:
		return v, true
	case int32:
		return int(v), true
	case int64:
		return int(v), true
	case float64:
		return int(v), float64(int(v)) == v
	case float32:
		return int(v), float32(int(v)) == v
	case string:
		n, err := strconv.Atoi(strings.TrimSpace(v))
		if err != nil {
			return 0, false
		}
		return n, true
	default:
		return 0, false
	}
}

func parseViduBool(raw any) (bool, bool) {
	switch v := raw.(type) {
	case bool:
		return v, true
	case string:
		s := strings.ToLower(strings.TrimSpace(v))
		if s == "true" {
			return true, true
		}
		if s == "false" {
			return false, true
		}
	}
	return false, false
}

func containsInt(sl []int, target int) bool {
	for _, v := range sl {
		if v == target {
			return true
		}
	}
	return false
}

func joinInts(sl []int) string {
	if len(sl) == 0 {
		return ""
	}
	parts := make([]string, 0, len(sl))
	for _, v := range sl {
		parts = append(parts, strconv.Itoa(v))
	}
	return strings.Join(parts, ", ")
}

func containsFold(sl []string, target string) bool {
	for _, v := range sl {
		if strings.EqualFold(strings.TrimSpace(v), strings.TrimSpace(target)) {
			return true
		}
	}
	return false
}

func (g *Generation) validateViduImage(ref string) error {
	ref = strings.TrimSpace(ref)
	if !isValidViduRefImage(ref) {
		return errs.New(400, "Vidu images 仅支持 1 张 http(s) 图片地址或 data:image base64")
	}
	if strings.HasPrefix(strings.ToLower(ref), "data:image/") {
		return validateViduDataImage(ref)
	}
	return g.validateViduRemoteImage(ref)
}

func validateViduDataImage(ref string) error {
	parts := strings.SplitN(ref, ",", 2)
	if len(parts) != 2 {
		return errs.New(400, "Vidu data:image 格式错误")
	}
	header := strings.ToLower(parts[0])
	if !strings.Contains(header, "image/jpeg") && !strings.Contains(header, "image/jpg") && !strings.Contains(header, "image/png") {
		return errs.New(400, "Vidu 仅支持 JPEG/PNG 参考图")
	}
	data, err := base64.StdEncoding.DecodeString(strings.TrimSpace(parts[1]))
	if err != nil {
		return errs.New(400, "Vidu 参考图 Base64 解码失败")
	}
	if len(data) == 0 || len(data) > viduMaxDataImageBytes {
		return errs.New(400, "Vidu 参考图体积需在 0-10MB 之间")
	}
	cfg, _, err := image.DecodeConfig(bytes.NewReader(data))
	if err != nil {
		return errs.New(400, "Vidu 参考图无法解析，请使用 JPEG/PNG 图片")
	}
	if cfg.Width <= 0 || cfg.Height <= 0 {
		return errs.New(400, "Vidu 参考图尺寸无效")
	}
	r := float64(cfg.Width) / float64(cfg.Height)
	if r < 0.4 || r > 2.5 {
		return errs.New(400, "Vidu 参考图比例不受支持，请使用 0.4-2.5 之间的宽高比")
	}
	return nil
}

func (g *Generation) validateViduRemoteImage(imageURL string) error {
	client := g.GW.HTTP
	if client == nil {
		client = http.DefaultClient
	}
	req, err := http.NewRequest(http.MethodGet, imageURL, nil)
	if err != nil {
		return errs.New(400, "Vidu 参考图地址不合法")
	}
	req.Header.Set("User-Agent", "aigc-server-go/vidu-validator")
	resp, err := client.Do(req)
	if err != nil {
		return errs.New(400, "Vidu 参考图地址不可访问")
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 400 {
		return errs.New(400, "Vidu 参考图地址不可访问")
	}
	ct := strings.ToLower(strings.TrimSpace(strings.Split(resp.Header.Get("Content-Type"), ";")[0]))
	if ct != "" && ct != "image/jpeg" && ct != "image/jpg" && ct != "image/png" {
		return errs.New(400, "Vidu 仅支持 JPEG/PNG 参考图")
	}
	buf, err := io.ReadAll(io.LimitReader(resp.Body, viduMaxRemoteImageSize+1))
	if err != nil {
		return errs.New(400, "Vidu 参考图读取失败")
	}
	if len(buf) == 0 || len(buf) > viduMaxRemoteImageSize {
		return errs.New(400, "Vidu 参考图体积需在 0-10MB 之间")
	}
	cfg, _, err := image.DecodeConfig(bytes.NewReader(buf))
	if err != nil {
		return errs.New(400, "Vidu 参考图无法解析，请使用 JPEG/PNG 图片")
	}
	if cfg.Width <= 0 || cfg.Height <= 0 {
		return errs.New(400, "Vidu 参考图尺寸无效")
	}
	r := float64(cfg.Width) / float64(cfg.Height)
	if r < 0.4 || r > 2.5 {
		return errs.New(400, "Vidu 参考图比例不受支持，请使用 0.4-2.5 之间的宽高比")
	}
	return nil
}

func (g *Generation) genVideos(prompt string, count int, requested, refURL string, viduOpts map[string]any) ([]string, string, string, string, string, error) {
	rm := g.resolveModel("video", requested)
	if rm != nil {
		if strings.EqualFold(rm.Provider.Key, "ark") {
			var vids []string
			for i := 0; i < count; i++ {
				u, err := g.callArkVideo(rm.Conn.BaseURL, rm.APIKey, prompt, rm.Model.ModelName)
				if err != nil {
					return nil, "", "", "", "", err
				}
				vids = append(vids, u)
			}
			return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
		}
		if strings.EqualFold(rm.Provider.Key, "moark") {
			if strings.TrimSpace(refURL) == "" {
				return nil, "", "", "", "", errs.New(400, "Moark 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址）")
			}
			var vids []string
			for i := 0; i < count; i++ {
				u, err := g.callMoarkVideo(rm.Provider, rm.Conn.BaseURL, rm.APIKey, rm.Meta, prompt, rm.Model.ModelName, strings.TrimSpace(refURL))
				if err != nil {
					return nil, "", "", "", "", err
				}
				vids = append(vids, u)
			}
			return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
		}
		if strings.EqualFold(rm.Provider.Key, "vidu") {
			if !isValidViduRefImage(refURL) {
				return nil, "", "", "", "", errs.New(400, "Vidu 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址，或 data:image/...;base64,...）")
			}
			viduPayload, err := g.buildViduPayload(rm, prompt, strings.TrimSpace(refURL), viduOpts)
			if err != nil {
				return nil, "", "", "", "", err
			}
			var vids []string
			for i := 0; i < count; i++ {
				u, err := g.callViduVideo(rm.Provider, rm.Conn.BaseURL, rm.APIKey, rm.Meta, viduPayload)
				if err != nil {
					return nil, "", "", "", "", err
				}
				vids = append(vids, u...)
			}
			return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
		}
		if strings.EqualFold(rm.Provider.Key, "vidu_onelink") {
			if !isValidViduRefImage(refURL) {
				return nil, "", "", "", "", errs.New(400, "Vidu 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址，或 data:image/...;base64,...）")
			}
			viduPayload, err := g.buildViduPayload(rm, prompt, strings.TrimSpace(refURL), viduOpts)
			if err != nil {
				return nil, "", "", "", "", err
			}
			var vids []string
			for i := 0; i < count; i++ {
				u, err := g.callViduVideo(rm.Provider, rm.Conn.BaseURL, rm.APIKey, rm.Meta, viduPayload)
				if err != nil {
					return nil, "", "", "", "", err
				}
				vids = append(vids, u...)
			}
			return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
		}
		if strings.EqualFold(rm.Provider.Key, "onelinkai") {
			if isViduWorkspaceModel(rm.Model.ModelName) {
				if !isValidViduRefImage(refURL) {
					return nil, "", "", "", "", errs.New(400, "Vidu 图生视频需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址，或 data:image/...;base64,...）")
				}
				viduPayload, err := g.buildViduPayload(rm, prompt, strings.TrimSpace(refURL), viduOpts)
				if err != nil {
					return nil, "", "", "", "", err
				}
				var vids []string
				for i := 0; i < count; i++ {
					u, err := g.callViduVideo(viduOneLinkVideoProvider, rm.Conn.BaseURL, rm.APIKey, rm.Meta, viduPayload)
					if err != nil {
						return nil, "", "", "", "", err
					}
					vids = append(vids, u...)
				}
				return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
			}
			if isKlingModel(rm.Model.ModelName) {
				var vids []string
				for i := 0; i < count; i++ {
					u, err := g.callKlingVideo(rm.Conn.BaseURL, rm.APIKey, rm.Meta, prompt, rm.Model.ModelName, strings.TrimSpace(refURL))
					if err != nil {
						return nil, "", "", "", "", err
					}
					vids = append(vids, u)
				}
				return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
			}
			return nil, "", "", "", "", errs.New(400, "当前 OneLink 视频模型仅支持 Vidu 图生视频（viduq* 等）或 Kling 文生/图生视频；其它模型请使用方舟或等待后续接入")
		}
		if strings.EqualFold(rm.Provider.Key, "kling") {
			var vids []string
			for i := 0; i < count; i++ {
				u, err := g.callKlingVideo(rm.Conn.BaseURL, rm.APIKey, rm.Meta, prompt, rm.Model.ModelName, strings.TrimSpace(refURL))
				if err != nil {
					return nil, "", "", "", "", err
				}
				vids = append(vids, u)
			}
			return vids, rm.Model.ModelName, rm.Source, rm.Matched, rm.Reject, nil
		}
		return nil, "", "", "", "", errs.New(400, "当前视频模型仅支持配置为方舟(ark)、Moark(moark)、Vidu(vidu)、Kling(kling) 或 OneLink+Vidu/Kling 连接")
	}
	if strings.TrimSpace(requested) != "" {
		return nil, "", "", "", "", errs.New(400, "视频模型未在可用配置中")
	}
	modelName := g.Cfg.ArkDefaultVideoModel
	var vids []string
	for i := 0; i < count; i++ {
		u, err := g.callArkVideo(g.Cfg.ArkBaseURL, g.Cfg.ArkAPIKey, prompt, modelName)
		if err != nil {
			return nil, "", "", "", "", err
		}
		vids = append(vids, u)
	}
	return vids, modelName, "SYSTEM_FALLBACK", "default-video", "", nil
}

func (g *Generation) callArkVideo(baseURL, apiKey, prompt, videoModel string) (string, error) {
	p, _ := g.Cat.Require("ark")
	duration := g.Cfg.ArkVideoDurationSeconds
	if duration < 1 {
		duration = 5
	}
	if duration > 30 {
		duration = 30
	}
	text := strings.TrimSpace(prompt) + fmt.Sprintf(" --duration %d --camerafixed false --watermark %v", duration, g.Cfg.ArkWatermark)
	payload := map[string]any{
		"model": videoModel,
		"content": []any{
			map[string]any{"type": "text", "text": text},
		},
	}
	submit, err := g.GW.SubmitVideoTask(context.Background(), p, baseURL, apiKey, nil, payload, 60*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
		}
		return "", err
	}
	if u := parseArkVideoURL(submit, false); u != "" {
		return u, nil
	}
	tid := parseArkVideoTaskID(submit)
	if tid == "" {
		return "", errs.New(502, "视频模型服务返回异常，缺少task_id或视频地址")
	}
	return g.pollArkVideo(p, baseURL, apiKey, tid)
}

func (g *Generation) callMoarkVideo(def *catalog.Provider, baseURL, apiKey string, meta map[string]any, prompt, modelName, imageURL string) (string, error) {
	payload := map[string]any{
		"prompt":              prompt,
		"model":               modelName,
		"num_inference_steps": 50,
		"num_frames":          81,
		"image":               imageURL,
	}
	submit, err := g.GW.SubmitVideoTask(context.Background(), def, baseURL, apiKey, meta, payload, 120*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
		}
		return "", err
	}
	if u := parseArkVideoURL(submit, false); u != "" {
		return u, nil
	}
	tid := parseArkVideoTaskID(submit)
	if tid == "" {
		return "", errs.New(502, "Moark 未返回 task_id 或视频地址")
	}
	return g.pollArkVideo(def, baseURL, apiKey, tid)
}

func (g *Generation) callKlingVideo(baseURL, apiKey string, meta map[string]any, prompt, modelName, refImageURL string) (string, error) {
	imageToVideo := refImageURL != "" && strings.TrimSpace(refImageURL) != ""
	var payload map[string]any
	var resultPath string
	if imageToVideo {
		resultPath = klingResultPath()
		payload = map[string]any{
			"model":   modelName,
			"prompt":  strings.TrimSpace(prompt),
			"image":   refImageURL,
			"duration": g.safeVideoDuration(),
		}
	} else {
		resultPath = klingResultPath()
		payload = map[string]any{
			"model":   modelName,
			"prompt":  strings.TrimSpace(prompt),
			"duration": g.safeVideoDuration(),
		}
	}
	var submitPath string
	if imageToVideo {
		submitPath = klingImageToVideoPath()
	} else {
		submitPath = klingTextToVideoPath()
	}
	submit, err := g.GW.PostJSON(context.Background(), baseURL, submitPath, klingOneLinkVideoProvider, apiKey, meta, payload, 120*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
		}
		return "", err
	}
	if u := parseArkVideoURL(submit, false); u != "" {
		return u, nil
	}
	tid := parseArkVideoTaskID(submit)
	if tid == "" {
		return "", errs.New(502, "Kling 视频生成未返回 task_id 或视频地址")
	}
	return g.pollKlingVideo(klingOneLinkVideoProvider, baseURL, apiKey, tid, resultPath)
}

func (g *Generation) safeVideoDuration() string {
	d := g.Cfg.ArkVideoDurationSeconds
	if d < 1 {
		d = 5
	}
	if d > 30 {
		d = 30
	}
	return strconv.Itoa(d)
}

func (g *Generation) pollKlingVideo(def *catalog.Provider, baseURL, apiKey, taskID, resultPathTemplate string) (string, error) {
	max := g.Cfg.ArkVideoPollMaxAttempts
	if max < 1 {
		max = 40
	}
	interval := g.Cfg.ArkVideoPollIntervalMs
	if interval < 300 {
		interval = 300
	}
	_ = resultPathTemplate
	for attempt := 1; attempt <= max; attempt++ {
		res, err := g.GW.QueryVideoTask(context.Background(), def, baseURL, apiKey, taskID, 30*time.Second)
		if err != nil {
			if pe, ok := err.(*gateway.ProviderError); ok {
				return "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
			}
			return "", err
		}
		if errNode := res["error"]; errNode != nil && fmt.Sprint(errNode) != "false" && errNode != false {
			return "", errs.New(502, parseKlingTaskError(res))
		}
		if u := parseArkVideoURL(res, false); u != "" {
			return u, nil
		}
		st := parseArkTaskStatus(res)
		if isFailedStatus(st) {
			return "", errs.New(502, "视频任务失败："+parseKlingTaskError(res))
		}
		if attempt < max {
			time.Sleep(time.Duration(interval) * time.Millisecond)
		}
	}
	return "", errs.New(504, "视频生成超时，请稍后重试或缩短提示词")
}

func parseKlingTaskError(body map[string]any) string {
	if body == nil {
		return "未知错误"
	}
	return firstNonBlank(
		strVal(body["message"]),
		strVal(body["error"]),
		strVal(body["error_message"]),
		strVal(body["reason"]),
		"未知错误",
	)
}

func isViduWorkspaceModel(modelName string) bool {
	n := strings.ToLower(strings.TrimSpace(modelName))
	if n == "" {
		return false
	}
	if strings.HasPrefix(n, "viduq") || strings.HasPrefix(n, "vidu") {
		return true
	}
	return false
}

func isKlingModel(modelName string) bool {
	n := strings.ToLower(strings.TrimSpace(modelName))
	if n == "" {
		return false
	}
	if strings.HasPrefix(n, "kling-") {
		return true
	}
	return false
}

func isValidViduRefImage(ref string) bool {
	ref = strings.TrimSpace(ref)
	if ref == "" {
		return false
	}
	if isHTTPURL(ref) {
		return true
	}
	low := strings.ToLower(ref)
	return strings.HasPrefix(low, "data:image/") && strings.Contains(ref, "base64")
}

func (g *Generation) callViduVideo(def *catalog.Provider, baseURL, apiKey string, meta map[string]any, payload map[string]any) ([]string, error) {
	submit, err := g.GW.SubmitVideoTask(context.Background(), def, baseURL, apiKey, meta, payload, 120*time.Second)
	if err != nil {
		if pe, ok := err.(*gateway.ProviderError); ok {
			return nil, errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
		}
		return nil, err
	}
	if urls := flattenViduVideoURLs(submit); len(urls) > 0 {
		return urls, nil
	}
	if u := parseArkVideoURL(submit, false); u != "" {
		return []string{u}, nil
	}
	tid := parseArkVideoTaskID(submit)
	if tid == "" {
		return nil, errs.New(502, "Vidu 未返回 task_id 或视频地址")
	}
	return g.pollViduTask(def, baseURL, apiKey, tid)
}

func (g *Generation) pollViduTask(def *catalog.Provider, baseURL, apiKey string, taskID string) ([]string, error) {
	max := g.Cfg.ArkVideoPollMaxAttempts
	if max < 1 {
		max = 40
	}
	interval := g.Cfg.ArkVideoPollIntervalMs
	if interval < 300 {
		interval = 300
	}
	for attempt := 1; attempt <= max; attempt++ {
		res, err := g.GW.QueryVideoTask(context.Background(), def, baseURL, apiKey, taskID, 30*time.Second)
		if err != nil {
			if pe, ok := err.(*gateway.ProviderError); ok {
				return nil, errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
			}
			return nil, err
		}
		if errNode := res["error"]; errNode != nil && fmt.Sprint(errNode) != "false" && errNode != false {
			return nil, errs.New(502, parseViduTaskError(res))
		}
		if urls := flattenViduVideoURLs(res); len(urls) > 0 {
			return urls, nil
		}
		st := parseArkTaskStatus(res)
		if isFailedStatus(st) {
			return nil, errs.New(502, "视频任务失败："+parseViduTaskError(res))
		}
		if attempt < max {
			time.Sleep(time.Duration(interval) * time.Millisecond)
		}
	}
	return nil, errs.New(504, "视频生成超时，请稍后重试或缩短提示词")
}

func parseViduPrimaryVideoURL(body map[string]any) string {
	if body == nil {
		return ""
	}
	if c, ok := body["creations"].([]any); ok && len(c) > 0 {
		if first, ok := c[0].(map[string]any); ok {
			if u := strVal(first["url"]); isHTTPURL(u) {
				return u
			}
		}
	}
	return parseArkVideoURL(body, false)
}

func parseViduVideoURLs(body map[string]any) (primary string, watermark string) {
	if body == nil {
		return "", ""
	}
	if c, ok := body["creations"].([]any); ok && len(c) > 0 {
		if first, ok := c[0].(map[string]any); ok {
			for k, v := range first {
				key := strings.ToLower(strings.TrimSpace(k))
				val := strings.TrimSpace(fmt.Sprint(v))
				if !isHTTPURL(val) {
					continue
				}
				isWatermarkKey := strings.Contains(key, "watermark") || strings.Contains(key, "wm_") || strings.Contains(key, "wmurl")
				if isWatermarkKey {
					if watermark == "" {
						watermark = val
					}
					continue
				}
				isPrimaryKey := key == "url" || key == "video_url" || strings.Contains(key, "origin_video") || strings.Contains(key, "source_video")
				if isPrimaryKey && primary == "" {
					primary = val
				}
			}
		}
	}
	if primary == "" {
		primary = parseViduPrimaryVideoURL(body)
	}
	return primary, watermark
}

func flattenViduVideoURLs(body map[string]any) []string {
	primary, watermark := parseViduVideoURLs(body)
	var out []string
	for _, u := range []string{primary, watermark} {
		u = strings.TrimSpace(u)
		if u == "" {
			continue
		}
		if len(out) > 0 && out[len(out)-1] == u {
			continue
		}
		dup := false
		for _, e := range out {
			if e == u {
				dup = true
				break
			}
		}
		if !dup {
			out = append(out, u)
		}
	}
	return out
}

func parseViduTaskError(body map[string]any) string {
	if body == nil {
		return "未知错误"
	}
	if c, ok := body["creations"].([]any); ok && len(c) > 0 {
		if first, ok := c[0].(map[string]any); ok {
			msg := firstNonBlank(
				strVal(first["message"]),
				strVal(first["error"]),
				strVal(first["error_message"]),
				strVal(first["reason"]),
			)
			if msg != "" {
				return msg
			}
		}
	}
	return parseArkTaskError(body)
}

func (g *Generation) pollArkVideo(def *catalog.Provider, baseURL, apiKey, taskID string) (string, error) {
	max := g.Cfg.ArkVideoPollMaxAttempts
	if max < 1 {
		max = 40
	}
	interval := g.Cfg.ArkVideoPollIntervalMs
	if interval < 300 {
		interval = 300
	}
	for attempt := 1; attempt <= max; attempt++ {
		res, err := g.GW.QueryVideoTask(context.Background(), def, baseURL, apiKey, taskID, 30*time.Second)
		if err != nil {
			if pe, ok := err.(*gateway.ProviderError); ok {
				return "", errs.New(mapProviderStatus(pe.StatusCode), pe.Message)
			}
			return "", err
		}
		if errNode := res["error"]; errNode != nil && fmt.Sprint(errNode) != "false" && errNode != false {
			return "", errs.New(502, parseArkTaskError(res))
		}
		if u := parseArkVideoURL(res, false); u != "" {
			return u, nil
		}
		st := parseArkTaskStatus(res)
		if isFailedStatus(st) {
			return "", errs.New(502, "视频任务失败："+parseArkTaskError(res))
		}
		if attempt < max {
			time.Sleep(time.Duration(interval) * time.Millisecond)
		}
	}
	return "", errs.New(504, "视频生成超时，请稍后重试或缩短提示词")
}

func parseArkVideoURL(body map[string]any, strict bool) string {
	urls := collectVideoURLs(body, 0)
	if len(urls) > 0 {
		return urls[0]
	}
	if strict {
		return ""
	}
	return ""
}

func collectVideoURLs(node any, depth int) []string {
	if node == nil || depth > 8 {
		return nil
	}
	if s, ok := node.(string); ok && isHTTPURL(s) {
		return []string{s}
	}
	m, ok := node.(map[string]any)
	if !ok {
		return nil
	}
	var out []string
	for k, v := range m {
		lk := strings.ToLower(k)
		if strings.Contains(lk, "url") {
			if s := fmt.Sprint(v); isHTTPURL(s) {
				out = append(out, s)
			}
		}
		out = append(out, collectVideoURLs(v, depth+1)...)
	}
	return out
}

func isHTTPURL(s string) bool {
	s = strings.TrimSpace(strings.ToLower(s))
	return strings.HasPrefix(s, "http://") || strings.HasPrefix(s, "https://")
}

func parseArkVideoTaskID(body map[string]any) string {
	if body == nil {
		return ""
	}
	candidates := []string{
		strVal(body["task_id"]), strVal(body["taskId"]), strVal(body["id"]),
	}
	for _, c := range candidates {
		if c != "" {
			return c
		}
	}
	return ""
}

func parseArkTaskStatus(body map[string]any) string {
	if body == nil {
		return ""
	}
	return firstNonBlank(strVal(body["status"]), strVal(body["task_status"]), strVal(body["state"]))
}

func parseArkTaskError(body map[string]any) string {
	if body == nil {
		return "未知错误"
	}
	return firstNonBlank(strVal(body["message"]), strVal(body["error"]), strVal(body["error_message"]), strVal(body["err_code"]), "未知错误")
}

func firstNonBlank(vals ...string) string {
	for _, v := range vals {
		if strings.TrimSpace(v) != "" {
			return v
		}
	}
	return ""
}

func isFailedStatus(status string) bool {
	s := strings.ToLower(strings.TrimSpace(status))
	switch s {
	case "failed", "error", "cancelled", "canceled", "timeout":
		return true
	}
	return false
}

func (g *Generation) persistWorkspace(taskID string, task *model.GenerationTask, images, videos []string) {
	hasImg := len(images) > 0
	hasVid := len(videos) > 0
	if !hasImg && !hasVid {
		task.PersistedImageFileIDsJSON = "[]"
		task.PersistedVideoFileIDsJSON = "[]"
		return
	}
	defer func() { recover() }()
	agg, err := g.Script.Require(consts.WorkspaceProjectID)
	if err != nil {
		task.PersistedImageFileIDsJSON = "[]"
		task.PersistedVideoFileIDsJSON = "[]"
		return
	}
	var pImg, pVid []string
	var dispImg, dispVid []string
	for i, url := range images {
		url = strings.TrimSpace(url)
		if url == "" {
			dispImg = append(dispImg, "")
			continue
		}
		if strings.HasPrefix(url, "http://") || strings.HasPrefix(url, "https://") {
			rec, err := g.Local.StoreRemote(consts.WorkspaceProjectID, fmt.Sprintf("workspace-gen/%s/img-%d%s", taskID, i, guessImgExt(url)), guessImgMIME(url), url)
			if err == nil {
				g.Script.UpsertFile(agg, rec)
				pImg = append(pImg, rec.FileID)
				dispImg = append(dispImg, g.Local.ToPublicURL(rec.FileID))
				continue
			}
		}
		rec := g.Local.StoreBase64(consts.WorkspaceProjectID, fmt.Sprintf("workspace-gen/%s/img-%d.png", taskID, i), "image/png", url)
		g.Script.UpsertFile(agg, rec)
		pImg = append(pImg, rec.FileID)
		dispImg = append(dispImg, g.Local.ToPublicURL(rec.FileID))
	}
	for i, url := range videos {
		url = strings.TrimSpace(url)
		if url == "" {
			dispVid = append(dispVid, "")
			continue
		}
		if !strings.HasPrefix(url, "http://") && !strings.HasPrefix(url, "https://") {
			dispVid = append(dispVid, url)
			continue
		}
		rec, err := g.Local.StoreRemote(consts.WorkspaceProjectID, fmt.Sprintf("workspace-gen/%s/vid-%d.mp4", taskID, i), "video/mp4", url)
		if err != nil {
			dispVid = append(dispVid, url)
			continue
		}
		g.Script.UpsertFile(agg, rec)
		pVid = append(pVid, rec.FileID)
		dispVid = append(dispVid, g.Local.ToPublicURL(rec.FileID))
	}
	_ = g.Script.Save(agg)
	task.PersistedImageFileIDsJSON = jsonutil.ToJSONStringSlice(pImg)
	task.PersistedVideoFileIDsJSON = jsonutil.ToJSONStringSlice(pVid)
	if len(dispImg) > 0 {
		task.ImageResultsJSON = jsonutil.ToJSONStringSlice(dispImg)
	}
	if len(dispVid) > 0 {
		task.VideoResultsJSON = jsonutil.ToJSONStringSlice(dispVid)
	}
}

func guessImgExt(url string) string {
	l := strings.ToLower(url)
	switch {
	case strings.Contains(l, ".png"):
		return ".png"
	case strings.Contains(l, ".webp"):
		return ".webp"
	case strings.Contains(l, ".gif"):
		return ".gif"
	case strings.Contains(l, ".jpeg"), strings.Contains(l, ".jpg"):
		return ".jpg"
	default:
		return ".png"
	}
}

func guessImgMIME(url string) string {
	l := strings.ToLower(url)
	switch {
	case strings.Contains(l, ".png"):
		return "image/png"
	case strings.Contains(l, ".webp"):
		return "image/webp"
	case strings.Contains(l, ".gif"):
		return "image/gif"
	case strings.Contains(l, ".jpeg"), strings.Contains(l, ".jpg"):
		return "image/jpeg"
	default:
		return "image/png"
	}
}

func (g *Generation) listEnabledByCapability(cap string) []model.ModelConfig {
	all, err := g.Store.AllModels()
	if err != nil {
		return nil
	}
	var out []model.ModelConfig
	for i := range all {
		if !all[i].Enabled {
			continue
		}
		if g.Cap.Supports(&all[i], cap) {
			out = append(out, all[i])
		}
	}
	return out
}

func distinctModelNames(rows []model.ModelConfig) []string {
	seen := map[string]struct{}{}
	var out []string
	for _, r := range rows {
		n := strings.TrimSpace(r.ModelName)
		if n == "" {
			continue
		}
		if _, ok := seen[n]; ok {
			continue
		}
		seen[n] = struct{}{}
		out = append(out, n)
	}
	return out
}

func sliceContains(sl []string, v string) bool {
	for _, s := range sl {
		if s == v {
			return true
		}
	}
	return false
}

func (g *Generation) resolveDefaultModelName(configured []model.ModelConfig, options []string) string {
	ordered, _ := g.Routing.ResolveOrderedConnectionConfigs(true)
	for _, conn := range ordered {
		for _, m := range configured {
			if m.ConnectionID == conn.ID {
				return m.ModelName
			}
		}
	}
	if len(options) > 0 {
		return options[0]
	}
	return ""
}

func (g *Generation) buildModelDetails(configured []model.ModelConfig, capability string) []any {
	conns, err := g.Store.AllConnections()
	if err != nil {
		return []any{}
	}
	byID := make(map[string]model.ConnectionConfig, len(conns))
	for _, c := range conns {
		byID[c.ID] = c
	}
	var out []any
	for _, m := range configured {
		conn := byID[m.ConnectionID]
		out = append(out, map[string]any{
			"modelName":         m.ModelName,
			"displayName":       m.Name,
			"provider":          m.Provider,
			"capability":        capability,
			"enabled":           m.Enabled,
			"connectionEnabled": conn.Enabled,
		})
	}
	return out
}

// ImageModelOptions matches Java GenerationController.imageModels (DB models + Ark fallback).
func (g *Generation) ImageModelOptions() map[string]any {
	configured := g.listEnabledByCapability("image")
	if len(configured) == 0 {
		opts := append([]string(nil), config.ArkImageModelOptionsEnv()...)
		def := g.Cfg.ArkDefaultImageModel
		if !sliceContains(opts, def) {
			opts = append([]string{def}, opts...)
		}
		return map[string]any{"defaultModel": def, "options": opts, "details": []any{}}
	}
	options := distinctModelNames(configured)
	def := g.resolveDefaultModelName(configured, options)
	return map[string]any{
		"defaultModel": def,
		"options":      options,
		"details":      g.buildModelDetails(configured, "image"),
	}
}

// VideoModelOptions matches Java GenerationController.videoModels.
func (g *Generation) VideoModelOptions() map[string]any {
	configured := g.listEnabledByCapability("video")
	if len(configured) == 0 {
		opts := append([]string(nil), config.ArkVideoModelOptionsEnv()...)
		def := g.Cfg.ArkDefaultVideoModel
		if !sliceContains(opts, def) {
			opts = append([]string{def}, opts...)
		}
		return map[string]any{"defaultModel": def, "options": opts, "details": []any{}}
	}
	options := distinctModelNames(configured)
	def := g.resolveDefaultModelName(configured, options)
	return map[string]any{
		"defaultModel": def,
		"options":      options,
		"details":      g.buildModelDetails(configured, "video"),
	}
}

func (g *Generation) taskToMap(t *model.GenerationTask) map[string]any {
	created := t.CreatedAt.Format("2006-01-02T15:04:05")
	return map[string]any{
		"taskId":                 t.TaskID,
		"status":                 t.Status,
		"textResults":            jsonutil.StringSliceFromJSON(t.TextResultsJSON),
		"imageResults":           jsonutil.StringSliceFromJSON(t.ImageResultsJSON),
		"videoResults":           jsonutil.StringSliceFromJSON(t.VideoResultsJSON),
		"createdAt":              created,
		"latencyMs":              t.LatencyMs,
		"prompt":                 t.Prompt,
		"mode":                   t.Mode,
		"style":                  t.Style,
		"imageModel":             t.ImageModel,
		"videoModel":             t.VideoModel,
		"imageModelSource":       t.ImageModelSource,
		"videoModelSource":       t.VideoModelSource,
		"imageModelMatchedBy":    t.ImageModelMatchedBy,
		"videoModelMatchedBy":    t.VideoModelMatchedBy,
		"imageModelRejectReason": t.ImageModelRejectReason,
		"videoModelRejectReason": t.VideoModelRejectReason,
		"persistedImageFileIds":  jsonutil.StringSliceFromJSON(t.PersistedImageFileIDsJSON),
		"persistedVideoFileIds":  jsonutil.StringSliceFromJSON(t.PersistedVideoFileIDsJSON),
	}
}

func (g *Generation) History(page, pageSize int, mode *string, owner string) (map[string]any, error) {
	if page < 1 {
		page = 1
	}
	if pageSize < 1 {
		pageSize = 1
	}
	if pageSize > 50 {
		pageSize = 50
	}
	list, err := g.Store.ListTasksForPage(owner, mode, page, pageSize)
	if err != nil {
		return nil, err
	}
	var items []any
	for i := range list {
		items = append(items, g.taskToMap(&list[i]))
	}
	total := g.Store.CountTasks(owner, mode)
	return map[string]any{"list": items, "total": total}, nil
}

func (g *Generation) TaskDetail(taskID, owner string) (map[string]any, error) {
	t, err := g.Store.GetTask(taskID)
	if err != nil {
		return nil, errs.New(404, "任务不存在")
	}
	if t.OwnerID != owner {
		return nil, errs.New(403, "无权访问该任务")
	}
	return g.taskToMap(t), nil
}

func (g *Generation) DeleteTask(taskID, owner string) error {
	t, err := g.Store.GetTask(taskID)
	if err != nil {
		return nil
	}
	if t.OwnerID != owner {
		return errs.New(403, "无权删除该任务")
	}
	return g.Store.DeleteTask(taskID)
}

// VideoStyle minimal stand-in for VideoStylePresetRegistry
type VideoStyle struct{}

func (VideoStyle) NormalizeForWrite(style string) string    { return strings.TrimSpace(style) }
func (VideoStyle) AnchorByStyleKey(key string) string       { return key }
func (VideoStyle) VideoNegativeEnglish() string             { return "low quality" }
func (VideoStyle) ResolveAnchorForRead(style string) string { return style }
func (VideoStyle) NormalizeKeyForRead(style string) string  { return style }

func randomHex(n int) string {
	b := make([]byte, n)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}
