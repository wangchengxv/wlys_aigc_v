package model

import (
	"time"
)

// --- connection_config / model_config (JPA model package) ---

type ConnectionConfig struct {
	ID               string    `gorm:"column:id;primaryKey"`
	Name             string    `gorm:"column:name"`
	Provider         string    `gorm:"column:provider"`
	BaseURL          string    `gorm:"column:base_url"`
	EncryptedAPIKey  string    `gorm:"column:encrypted_api_key"`
	MetadataJSON     string    `gorm:"column:metadata_json;type:longtext"`
	Enabled          bool      `gorm:"column:enabled"`
	CreatedAt        time.Time `gorm:"column:created_at"`
	UpdatedAt        time.Time `gorm:"column:updated_at"`
}

func (ConnectionConfig) TableName() string { return "connection_config" }

type ModelConfig struct {
	ID           string    `gorm:"column:id;primaryKey"`
	Name         string    `gorm:"column:name"`
	Provider     string    `gorm:"column:provider"`
	ModelName    string    `gorm:"column:model_name"`
	ConnectionID string    `gorm:"column:connection_id"`
	Enabled      bool      `gorm:"column:enabled"`
	MetadataJSON string    `gorm:"column:metadata_json;type:longtext"`
	CreatedAt    time.Time `gorm:"column:created_at"`
	UpdatedAt    time.Time `gorm:"column:updated_at"`
}

func (ModelConfig) TableName() string { return "model_config" }

type RoutingConfig struct {
	ID                         int64  `gorm:"column:id;primaryKey"`
	Strategy                   string `gorm:"column:strategy"`
	PriorityConnectionIDsJSON  string `gorm:"column:priority_connection_ids_json;type:longtext"`
	FailoverEnabled            bool   `gorm:"column:failover_enabled"`
	FailoverTimeoutSeconds     int    `gorm:"column:failover_timeout_seconds"`
	TimeScheduleJSON           string `gorm:"column:time_schedule_json;type:longtext"`
}

func (RoutingConfig) TableName() string { return "routing_config" }

type RouterAPIKey struct {
	ID          string     `gorm:"column:id;primaryKey"`
	Name        string     `gorm:"column:name"`
	KeyValue    string     `gorm:"column:key_value"`
	Active      bool       `gorm:"column:active"`
	CreatedAt   time.Time  `gorm:"column:created_at"`
	LastUsedAt  *time.Time `gorm:"column:last_used_at"`
}

func (RouterAPIKey) TableName() string { return "router_api_key" }

type RouterRequestLog struct {
	ID               string    `gorm:"column:id;primaryKey"`
	Timestamp        time.Time `gorm:"column:timestamp"`
	RouterAPIKeyID   string    `gorm:"column:router_api_key_id"`
	ConnectionID     string    `gorm:"column:connection_id"`
	ConnectionName   string    `gorm:"column:connection_name"`
	Provider         string    `gorm:"column:provider"`
	Model            string    `gorm:"column:model"`
	RequestFormat    string    `gorm:"column:request_format"`
	Status           string    `gorm:"column:status"`
	DurationMs       int       `gorm:"column:duration_ms"`
	PromptTokens     int       `gorm:"column:prompt_tokens"`
	CompletionTokens int       `gorm:"column:completion_tokens"`
	TotalTokens      int       `gorm:"column:total_tokens"`
	ErrorMessage     string    `gorm:"column:error_message;type:text"`
}

func (RouterRequestLog) TableName() string { return "router_request_log" }

// --- generation_task ---

type GenerationTask struct {
	TaskID                   string    `gorm:"column:task_id;primaryKey"`
	OwnerID                  string    `gorm:"column:owner_id"`
	Prompt                   string    `gorm:"column:prompt;type:longtext"`
	Mode                     string    `gorm:"column:mode"`
	Style                    string    `gorm:"column:style"`
	ImageModel               string    `gorm:"column:image_model"`
	VideoModel               string    `gorm:"column:video_model"`
	ImageModelSource         string    `gorm:"column:image_model_source"`
	VideoModelSource         string    `gorm:"column:video_model_source"`
	ImageModelMatchedBy      string    `gorm:"column:image_model_matched_by"`
	VideoModelMatchedBy      string    `gorm:"column:video_model_matched_by"`
	ImageModelRejectReason   string    `gorm:"column:image_model_reject_reason"`
	VideoModelRejectReason   string    `gorm:"column:video_model_reject_reason"`
	Status                   string    `gorm:"column:status"`
	LatencyMs                int64     `gorm:"column:latency_ms"`
	ErrorCode                string    `gorm:"column:error_code"`
	CreatedAt                time.Time `gorm:"column:created_at"`
	TextResultsJSON          string    `gorm:"column:text_results_json;type:longtext"`
	ImageResultsJSON         string    `gorm:"column:image_results_json;type:longtext"`
	VideoResultsJSON         string    `gorm:"column:video_results_json;type:longtext"`
	PersistedImageFileIDsJSON  string `gorm:"column:persisted_image_file_ids_json;type:longtext"`
	PersistedVideoFileIDsJSON  string `gorm:"column:persisted_video_file_ids_json;type:longtext"`
}

func (GenerationTask) TableName() string { return "generation_task" }

// --- script aggregate tables ---

type ScriptProject struct {
	ProjectID                 string     `gorm:"column:project_id;primaryKey"`
	Name                      string     `gorm:"column:name"`
	Status                    string     `gorm:"column:status"`
	SourceType                string     `gorm:"column:source_type"`
	OriginalScriptFileID      string     `gorm:"column:original_script_file_id"`
	RefinedScriptFileID       string     `gorm:"column:refined_script_file_id"`
	RefinedScriptJSONFileID   string     `gorm:"column:refined_script_json_file_id"`
	UploadedSourceFileID      string     `gorm:"column:uploaded_source_file_id"`
	ScriptSummary             string     `gorm:"column:script_summary;type:text"`
	VisualStyle               string     `gorm:"column:visual_style;type:longtext"`
	AspectRatio               string     `gorm:"column:aspect_ratio"`
	TargetDuration            *int       `gorm:"column:target_duration"`
	Language                  string     `gorm:"column:language"`
	ExplicitTextModel         string     `gorm:"column:explicit_text_model"`
	ExplicitImageModel        string     `gorm:"column:explicit_image_model"`
	ExplicitVideoModel        string     `gorm:"column:explicit_video_model"`
	ArtDirectionJSON          string     `gorm:"column:art_direction_json;type:longtext"`
	WorkflowModelOverrides    string     `gorm:"column:workflow_model_overrides;type:longtext"`
	PromptTemplateOverrides   string     `gorm:"column:prompt_template_overrides;type:longtext"`
	CreatedAt                 time.Time  `gorm:"column:created_at"`
	UpdatedAt                 time.Time  `gorm:"column:updated_at"`
	DeletedAt                 *time.Time `gorm:"column:deleted_at"`
}

func (ScriptProject) TableName() string { return "script_project" }

type ScriptRevision struct {
	RevisionID          string    `gorm:"column:revision_id;primaryKey"`
	ProjectID           string    `gorm:"column:project_id"`
	RevisionIndex       int       `gorm:"column:revision_index"`
	Label               string    `gorm:"column:label"`
	Kind                string    `gorm:"column:kind"`
	CreatedAt           time.Time `gorm:"column:created_at"`
	RefinedMarkdownFileID string  `gorm:"column:refined_markdown_file_id"`
	RefinedJSONFileID     string  `gorm:"column:refined_json_file_id"`
}

func (ScriptRevision) TableName() string { return "script_revision" }

type ScriptDocumentVersion struct {
	DocumentID    string    `gorm:"column:document_id;primaryKey"`
	ProjectID     string    `gorm:"column:project_id"`
	VersionType   string    `gorm:"column:version_type"`
	Format        string    `gorm:"column:format"`
	FileID        string    `gorm:"column:file_id"`
	ContentDigest string    `gorm:"column:content_digest"`
	CreatedAt     time.Time `gorm:"column:created_at"`
}

func (ScriptDocumentVersion) TableName() string { return "script_document_version" }

type StoredFileRecord struct {
	FileID       string    `gorm:"column:file_id;primaryKey"`
	ProjectID    string    `gorm:"column:project_id"`
	FileName     string    `gorm:"column:file_name"`
	RelativePath string    `gorm:"column:relative_path"`
	MediaType    string    `gorm:"column:media_type"`
	SizeBytes    int64     `gorm:"column:size_bytes"`
	CreatedAt    time.Time `gorm:"column:created_at"`
}

func (StoredFileRecord) TableName() string { return "stored_file_record" }

type ExtractedAsset struct {
	AssetID                  string    `gorm:"column:asset_id;primaryKey"`
	ProjectID                string    `gorm:"column:project_id"`
	AssetType                string    `gorm:"column:asset_type"`
	Name                     string    `gorm:"column:name"`
	Description              string    `gorm:"column:description;type:text"`
	SourceShotID             string    `gorm:"column:source_shot_id"`
	TagsJSON                 string    `gorm:"column:tags_json;type:longtext"`
	PromptDraft              string    `gorm:"column:prompt_draft;type:text"`
	Status                   string    `gorm:"column:status"`
	MetadataJSON             string    `gorm:"column:metadata_json;type:longtext"`
	VisualPrompt             string    `gorm:"column:visual_prompt;type:text"`
	TurnaroundPlanJSON       string    `gorm:"column:turnaround_plan_json;type:longtext"`
	TurnaroundImageFileID    string    `gorm:"column:turnaround_image_file_id"`
	StoryboardPlanJSON       string    `gorm:"column:storyboard_plan_json;type:longtext"`
	StoryboardTranslationsJSON string  `gorm:"column:storyboard_translations_json;type:longtext"`
	StoryboardPromptText     string    `gorm:"column:storyboard_prompt_text;type:longtext"`
	StoryboardImageFileID    string    `gorm:"column:storyboard_image_file_id"`
	ThreeViewImageFileID     string    `gorm:"column:three_view_image_file_id"`
	PromptVersionsJSON       string    `gorm:"column:prompt_versions_json;type:longtext"`
	CreatedAt                time.Time `gorm:"column:created_at"`
	UpdatedAt                time.Time `gorm:"column:updated_at"`
}

func (ExtractedAsset) TableName() string { return "extracted_asset" }

type KeyframeRecord struct {
	KeyframeID           string    `gorm:"column:keyframe_id;primaryKey"`
	ProjectID            string    `gorm:"column:project_id"`
	AssetID              string    `gorm:"column:asset_id"`
	ShotID               string    `gorm:"column:shot_id"`
	PromptText           string    `gorm:"column:prompt_text;type:text"`
	NegativePrompt       string    `gorm:"column:negative_prompt;type:text"`
	ImageFileID          string    `gorm:"column:image_file_id"`
	Selected             bool      `gorm:"column:selected"`
	Status               string    `gorm:"column:status"`
	ProviderTaskID       string    `gorm:"column:provider_task_id"`
	ModelName            string    `gorm:"column:model_name"`
	PromptVersionsJSON   string    `gorm:"column:prompt_versions_json;type:longtext"`
	CreatedAt            time.Time `gorm:"column:created_at"`
	UpdatedAt            time.Time `gorm:"column:updated_at"`
}

func (KeyframeRecord) TableName() string { return "keyframe_record" }

type StoryboardShot struct {
	ShotID                 string    `gorm:"column:shot_id;primaryKey"`
	ProjectID              string    `gorm:"column:project_id"`
	ParentShotID           string    `gorm:"column:parent_shot_id"`
	SequenceNo             int       `gorm:"column:sequence_no"`
	Title                  string    `gorm:"column:title"`
	ScriptText             string    `gorm:"column:script_text;type:longtext"`
	ActionSummary          string    `gorm:"column:action_summary;type:text"`
	CameraMovement         string    `gorm:"column:camera_movement"`
	TargetDurationSec      *int      `gorm:"column:target_duration_sec"`
	CharacterRefsJSON      string    `gorm:"column:character_refs_json;type:longtext"`
	BackgroundRefsJSON     string    `gorm:"column:background_refs_json;type:longtext"`
	PropRefsJSON           string    `gorm:"column:prop_refs_json;type:longtext"`
	KeyframeRefsJSON       string    `gorm:"column:keyframe_refs_json;type:longtext"`
	Status                 string    `gorm:"column:status"`
	VisualPrompt           string    `gorm:"column:visual_prompt;type:text"`
	StoryboardAssetID      string    `gorm:"column:storyboard_asset_id"`
	StoryboardImageFileID  string    `gorm:"column:storyboard_image_file_id"`
	StoryboardCropFileID   string    `gorm:"column:storyboard_crop_file_id"`
	StoryboardCropIndex    *int      `gorm:"column:storyboard_crop_index"`
	FirstFrameMode         string    `gorm:"column:first_frame_mode"`
	ShotType               string    `gorm:"column:shot_type"`
	CameraMove             string    `gorm:"column:camera_move"`
	Emotion                string    `gorm:"column:emotion"`
	PromptVersionsJSON     string    `gorm:"column:prompt_versions_json;type:longtext"`
	CreatedAt              time.Time `gorm:"column:created_at"`
	UpdatedAt              time.Time `gorm:"column:updated_at"`
}

func (StoryboardShot) TableName() string { return "storyboard_shot" }

type VideoSegmentTask struct {
	SegmentTaskID       string     `gorm:"column:segment_task_id;primaryKey"`
	ProjectID           string     `gorm:"column:project_id"`
	ShotID              string     `gorm:"column:shot_id"`
	RequestPayloadFileID string    `gorm:"column:request_payload_file_id"`
	ResultVideoFileID   string     `gorm:"column:result_video_file_id"`
	ProviderTaskID      string     `gorm:"column:provider_task_id"`
	Status              string     `gorm:"column:status"`
	RetryCount          int        `gorm:"column:retry_count"`
	ModelName           string     `gorm:"column:model_name"`
	StartedAt           *time.Time `gorm:"column:started_at"`
	FinishedAt          *time.Time `gorm:"column:finished_at"`
	ErrorMessage        string     `gorm:"column:error_message;type:text"`
}

func (VideoSegmentTask) TableName() string { return "video_segment_task" }

type PipelineRun struct {
	PipelineRunID string     `gorm:"column:pipeline_run_id;primaryKey"`
	ProjectID     string     `gorm:"column:project_id"`
	PipelineType  string     `gorm:"column:pipeline_type"`
	Status        string     `gorm:"column:status"`
	CurrentStage  string     `gorm:"column:current_stage"`
	TotalCount    int        `gorm:"column:total_count"`
	SuccessCount  int        `gorm:"column:success_count"`
	FailedCount   int        `gorm:"column:failed_count"`
	ErrorMessage  string     `gorm:"column:error_message;type:text"`
	CreatedAt     time.Time  `gorm:"column:created_at"`
	UpdatedAt     time.Time  `gorm:"column:updated_at"`
}

func (PipelineRun) TableName() string { return "pipeline_run" }

type CanvasGraph struct {
	ID           string    `gorm:"column:id;primaryKey"`
	OwnerID      string    `gorm:"column:owner_id"`
	ProjectID    string    `gorm:"column:project_id"`
	Title        string    `gorm:"column:title"`
	GraphJSON    string    `gorm:"column:graph_json;type:longtext"`
	ViewportJSON string    `gorm:"column:viewport_json"`
	CreatedAt    time.Time `gorm:"column:created_at"`
	UpdatedAt    time.Time `gorm:"column:updated_at"`
}

func (CanvasGraph) TableName() string { return "canvas_graph" }

type AssetGenerationHistory struct {
	ID                     int64     `gorm:"column:id;primaryKey;autoIncrement"`
	ProjectID              string    `gorm:"column:project_id"`
	AssetType              string    `gorm:"column:asset_type"`
	ReferenceID            string    `gorm:"column:reference_id"`
	FileID                 string    `gorm:"column:file_id"`
	PromptText             string    `gorm:"column:prompt_text;type:text"`
	ModelName              string    `gorm:"column:model_name"`
	GenerationParamsJSON   string    `gorm:"column:generation_params_json;type:longtext"`
	CreatedAt              time.Time `gorm:"column:created_at"`
}

func (AssetGenerationHistory) TableName() string { return "asset_generation_history" }
