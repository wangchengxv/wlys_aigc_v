export type GenerateMode = 'text' | 'image' | 'both' | 'video'

/** 全局设定：画面比例 */
export type GlobalAspectRatio = '16:9' | '9:16' | '4:3' | '3:4' | '1:1' | '21:9'

/** 全局设定：剧本类型 */
export type GlobalScriptType = '剧情演绎' | '真人解说'

/** 全局设定：模型策略 */
export type GlobalModelStrategy = '省钱优先' | '画质优先'

/** 全局设定：创作模式 */
export type GlobalCreationMode = '生视频模式' | '多参数生视频'

/** 全局设定：分镜生成布局 */
export type GlobalStoryboardLayout = '单' | '九宫格机位'

/** 全局设定：视觉风格来源 */
export type GlobalVisualStyleMode = 'preset' | 'custom'

/** 系统视频风格库单条（数据来自项目内风格文档） */
export interface VideoStylePreset {
  id: string
  category: string
  name: string
  traits: string
  fullPrompt: string
}

export interface GenerateRequest {
  prompt: string
  mode: GenerateMode
  style: string
  imageSize: string
  textLength: 'short' | 'medium' | 'long'
  count: number
  imageModel?: string
  videoModel?: string
  /** Moark 等图生视频：参考图 HTTP(S) URL */
  videoReferenceImageUrl?: string
}

export interface GenerateResponse {
  taskId: string
  status: 'SUCCESS' | 'FAIL' | 'PROCESSING'
  textResults: string[]
  imageResults: string[]
  videoResults: string[]
  createdAt: string
  latencyMs: number
  prompt: string
  mode: GenerateMode
  style: string
  imageModel?: string
  videoModel?: string
  /** 已落盘到服务端的图片 fileId 列表（与 imageResults 一一对应成功项） */
  persistedImageFileIds?: string[]
  persistedVideoFileIds?: string[]
}

export type AssetHistoryType =
  | 'KEYFRAME'
  | 'TURNAROUND'
  | 'STORYBOARD'
  | 'THREE_VIEW'
  | 'STORYBOARD_CROP'
  | 'GROUP_SCENE'
  | 'VIDEO'

export interface AssetGenerationHistoryItem {
  id: number
  projectId: string
  assetType: AssetHistoryType
  referenceId: string | null
  fileId: string
  promptText: string | null
  modelName: string | null
  generationParamsJson: string | null
  createdAt: string
}

export interface ImageModelOptions {
  defaultModel: string
  options: string[]
}

export interface VideoModelOptions {
  defaultModel: string
  options: string[]
}

export interface HistoryQuery {
  page: number
  pageSize: number
  mode?: GenerateMode | 'all'
}

export interface PagedTasks {
  list: GenerateResponse[]
  total: number
}

export interface ConnectionConfig {
  id: string
  name: string
  provider: string
  baseUrl: string
  apiKeyMasked: string
  hasApiKey: boolean
  enabled: boolean
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface ConnectionConfigCreateRequest {
  name: string
  provider: string
  baseUrl: string
  apiKey: string
  enabled: boolean
  metadata?: Record<string, unknown>
}

export interface ConnectionConfigUpdateRequest {
  name?: string
  provider?: string
  baseUrl?: string
  apiKey?: string
  enabled?: boolean
  metadata?: Record<string, unknown>
}

export interface ModelConfig {
  id: string
  name: string
  provider: string
  modelName: string
  connectionId: string
  enabled: boolean
  metadata: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface ModelConfigCreateRequest {
  name: string
  provider: string
  modelName: string
  connectionId: string
  enabled: boolean
  metadata?: Record<string, unknown>
}

export interface ModelConfigUpdateRequest {
  name?: string
  provider?: string
  modelName?: string
  connectionId?: string
  enabled?: boolean
  metadata?: Record<string, unknown>
}

export interface PresetModelDto {
  provider: string
  modelName: string
  baseUrl: string
  displayName: string
  capabilities: string[]
}

export interface PresetModelListResponse {
  models: PresetModelDto[]
  providers: string[]
}

export type ProviderCatalogAuthMode = 'BEARER' | 'X_API_KEY' | 'API_KEY_HEADER' | 'NONE'

export type ProviderGatewayKind =
  | 'OPENAI_COMPAT'
  | 'ANTHROPIC'
  | 'AZURE_OPENAI'
  | 'BEDROCK'
  | 'VERTEX'
  | 'OLLAMA'

export interface ProviderCatalogEntry {
  key: string
  displayName: string
  defaultBaseUrl: string
  authMode: ProviderCatalogAuthMode
  apiFormat: string
  gatewayKind: ProviderGatewayKind
  textProxySupported: boolean
  imageProxySupported: boolean
  videoProxySupported: boolean
  staticModels: string[]
}

export interface ProviderCatalogListResponse {
  providers: ProviderCatalogEntry[]
}

export interface QuickConnectionRequest {
  provider: string
  modelName: string
  apiKey: string
  enabled?: boolean
}

export interface ConnectionTestResponse {
  ok: boolean
  message: string
  models: string[]
}

export interface ModelProbeResponse {
  ok: boolean
  message: string
}

export interface BatchModelsImportRequest {
  connectionId: string
  modelNames: string[]
  capabilities?: string[]
}

export type ProjectStatus =
  | 'DRAFT'
  | 'SCRIPT_REFINING'
  | 'SCRIPT_READY'
  | 'ASSET_EXTRACTING'
  | 'ASSET_READY'
  | 'KEYFRAME_GENERATING'
  | 'KEYFRAME_READY'
  | 'VIDEO_GENERATING'
  | 'COMPLETED'
  | 'PARTIAL_FAILED'
  | 'FAILED'

export type AssetType = 'CHARACTER' | 'BACKGROUND' | 'PROP'
export type AssetStatus = 'PENDING' | 'EXTRACTED' | 'KEYFRAME_GENERATING' | 'KEYFRAME_READY' | 'CONFIRMED' | 'FAILED'
export type SegmentTaskStatus = 'PENDING' | 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED'
export type PipelineRunStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_FAILED' | 'FAILED'
export type PipelineType = 'REFINE' | 'ASSET_EXTRACTION' | 'KEYFRAME_GENERATION' | 'SHOT_SPLIT' | 'VIDEO_GENERATION'

export interface ScriptProject {
  projectId: string
  name: string
  status: ProjectStatus
  sourceType: string
  originalScriptFileId?: string | null
  refinedScriptFileId?: string | null
  refinedScriptJsonFileId?: string | null
  uploadedSourceFileId?: string | null
  scriptSummary?: string | null
  visualStyle: string
  aspectRatio: string
  targetDuration: number
  language: string
  explicitTextModel?: string | null
  explicitImageModel?: string | null
  explicitVideoModel?: string | null
  /** B-1 全局美术指导 JSON */
  artDirectionJson?: string | null
  createdAt: string
  updatedAt: string
}

export interface ScriptDocumentVersion {
  documentId: string
  projectId: string
  versionType: 'ORIGINAL' | 'REFINED_MARKDOWN' | 'REFINED_JSON' | 'UPLOADED_SOURCE'
  format: string
  fileId: string
  contentDigest: string
  createdAt: string
}

export interface StoredFileRecord {
  fileId: string
  projectId: string
  fileName: string
  relativePath: string
  mediaType: string
  sizeBytes: number
  createdAt: string
}

export type PromptVersionSource =
  | 'ai-generated'
  | 'manual-edit'
  | 'rollback'
  | 'imported'
  | 'system'

export interface PromptVersion {
  id: string
  prompt: string
  createdAt: number
  source: PromptVersionSource
  note?: string | null
}

export interface ExtractedAsset {
  assetId: string
  projectId: string
  assetType: AssetType
  name: string
  description: string
  sourceShotId?: string | null
  tags: string[]
  promptDraft: string
  /** B-2/B-3/B-4/B-5 生成的视觉提示词 */
  visualPrompt?: string | null
  promptVersions?: PromptVersion[] | null
  /** B-6 九宫格规划 JSON */
  turnaroundPlanJson?: string | null
  /** B-7 九宫格合成图文件 ID */
  turnaroundImageFileId?: string | null
  /** 九宫格分镜规划 JSON */
  storyboardPlanJson?: string | null
  /** 九宫格分镜翻译 JSON */
  storyboardTranslationsJson?: string | null
  /** 九宫格分镜图生成时使用的完整 prompt */
  storyboardPromptText?: string | null
  /** 九宫格分镜整图文件 ID */
  storyboardImageFileId?: string | null
  /** 1x3 三视图设定图文件 ID */
  threeViewImageFileId?: string | null
  status: AssetStatus
  metadata: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface KeyframeRecord {
  keyframeId: string
  projectId: string
  assetId: string
  shotId?: string | null
  promptText: string
  promptVersions?: PromptVersion[] | null
  negativePrompt?: string | null
  imageFileId?: string | null
  selected: boolean
  status: string
  providerTaskId?: string | null
  modelName?: string | null
  createdAt: string
  updatedAt: string
}

export interface StoryboardShot {
  shotId: string
  projectId: string
  parentShotId?: string | null
  sequenceNo: number
  title: string
  scriptText: string
  actionSummary: string
  cameraMovement: string
  /** B-9 结构化镜头类型（可选） */
  shotType?: string | null
  /** B-9 结构化运镜（可选） */
  cameraMove?: string | null
  /** B-9 情绪（可选） */
  emotion?: string | null
  characterRefs: string[]
  backgroundRefs: string[]
  propRefs: string[]
  keyframeRefs: string[]
  storyboardAssetId?: string | null
  storyboardImageFileId?: string | null
  storyboardCropFileId?: string | null
  storyboardCropIndex?: number | null
  firstFrameMode?: 'NONE' | 'FULL_GRID' | 'CROPPED_PANEL' | null
  /** B-9 分镜图像提示词 */
  visualPrompt?: string | null
  promptVersions?: PromptVersion[] | null
  targetDurationSec?: number | null
  status: string
  createdAt: string
  updatedAt: string
}

export interface UpdateShotRequest {
  shotType?: string
  cameraMove?: string
  emotion?: string
  targetDurationSec?: number
  visualPrompt?: string
}

export interface ArtDirectionResponse {
  artDirectionJson: string | null
}

export interface VisualPromptResponse {
  assetId: string
  visualPrompt: string
}

export interface BatchVisualPromptResponse {
  items: VisualPromptResponse[]
}

export interface TurnaroundPlanResponse {
  assetId: string
  turnaroundPlanJson: string
}

export interface TurnaroundImageResponse {
  assetId: string
  imageFileId: string
}

export interface StoryboardPlanResponse {
  assetId: string
  storyboardPlanJson: string
  storyboardTranslationsJson: string
}

export interface StoryboardImageResponse {
  assetId: string
  imageFileId: string
  promptText: string
}

export interface StoryboardPanelCropResponse {
  assetId: string
  panelIndex: number
  imageFileId: string
}

export interface StoryboardRewriteRequest {
  instruction: string
}

export interface ApplyStoryboardFirstFrameRequest {
  assetId?: string
  mode?: 'NONE' | 'FULL_GRID' | 'CROPPED_PANEL'
  panelIndex?: number
}

export interface StoryboardFirstFrameResponse {
  shotId: string
  mode: 'NONE' | 'FULL_GRID' | 'CROPPED_PANEL'
  assetId: string | null
  panelIndex: number | null
  imageFileId: string | null
}

export interface ThreeViewResponse {
  assetId: string
  imageFileId: string
}

export interface GenerateGroupSceneRequest {
  characterAssetIds: string[]
  location?: string
  time?: string
  atmosphere?: string
  generateImage?: boolean
}

export interface GroupSceneResponse {
  promptText: string
  imageFileId: string | null
}

export interface ShotVisualPromptResponse {
  shotId: string
  visualPrompt: string
}

export interface VideoSegmentTask {
  segmentTaskId: string
  projectId: string
  shotId: string
  requestPayloadFileId?: string | null
  resultVideoFileId?: string | null
  providerTaskId?: string | null
  status: SegmentTaskStatus
  retryCount: number
  modelName?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
}

export interface PipelineRun {
  pipelineRunId: string
  projectId: string
  pipelineType: PipelineType
  status: PipelineRunStatus
  currentStage: string
  totalCount: number
  successCount: number
  failedCount: number
  errorMessage?: string | null
  createdAt: string
  updatedAt: string
}

export type RevisionKind =
  | 'REFINE'
  | 'REWRITE'
  | 'USER_EDIT'
  | 'OPTIMIZE_SCENE'
  | 'OPTIMIZE_CHARACTER'
  | 'OPTIMIZE_PROP'
  | 'RESTORE'
  | 'IMPORT'
  | 'BEFORE_UPDATE'

export interface ScriptRevision {
  revisionId: string
  revisionIndex: number
  label: string
  kind: RevisionKind
  createdAt: string
  refinedMarkdownFileId?: string | null
  refinedJsonFileId?: string | null
}

export interface ScriptProjectAggregate {
  project: ScriptProject
  revisions?: ScriptRevision[]
  documents: ScriptDocumentVersion[]
  files: StoredFileRecord[]
  assets: ExtractedAsset[]
  keyframes: KeyframeRecord[]
  shots: StoryboardShot[]
  videoTasks: VideoSegmentTask[]
  pipelineRuns: PipelineRun[]
}

export interface ScriptProjectSummary {
  projectId: string
  name: string
  status: ProjectStatus
  scriptSummary?: string | null
  visualStyle: string
  aspectRatio: string
  targetDuration: number
  coverFileId?: string | null
  assetCount: number
  keyframeCount: number
  videoTaskCount: number
  createdAt: string
  updatedAt: string
}

export interface ScriptDocumentPayload {
  projectId: string
  originalText: string
  refinedMarkdown: string
  structuredScript: Record<string, unknown>
  documents: ScriptDocumentVersion[]
}

export interface PipelineStatus {
  projectId: string
  projectStatus: ProjectStatus
  latestRun?: PipelineRun | null
  totalCount: number
  successCount: number
  failedCount: number
  runningCount: number
  queuedCount: number
  pendingCount: number
}

export interface ScriptProjectCreateRequest {
  name: string
  sourceText: string
  visualStyle?: string
  aspectRatio?: string
  targetDuration?: number
  language?: string
  explicitTextModel?: string
  explicitImageModel?: string
  explicitVideoModel?: string
}

export interface ScriptProjectUploadRequest {
  name: string
  file: File
  visualStyle?: string
  aspectRatio?: string
  targetDuration?: number
  language?: string
  explicitTextModel?: string
  explicitImageModel?: string
  explicitVideoModel?: string
}

export interface UpdateScriptRequest {
  refinedMarkdown?: string
  structuredScript?: Record<string, unknown>
}

export interface UpdateAssetRequest {
  name?: string
  description?: string
  tags?: string[]
  promptDraft?: string
  metadata?: Record<string, unknown>
  visualPrompt?: string
}

export interface PromptTemplateCatalogItem {
  path: string
  title: string
  category: string
  description: string
  defaultBody: string
}

export interface AppendScriptPreviewRequest {
  maxAppendChars?: number
}

export interface AppendScriptPreviewResponse {
  baseUsed: 'refined' | 'original' | string
  existingLength: number
  maxAppendChars: number
  appendText: string
}

export interface RewriteScriptPreviewRequest {
  rewriteInstruction: string
  targetStyle?: string
  maxOutputChars?: number
  language?: string
}

export interface RewriteScriptPreviewResponse {
  baseUsed: 'refined' | 'original' | string
  sourceLength: number
  maxOutputChars?: number | null
  rewrittenText: string
}

export interface RewriteScriptApplyRequest {
  rewrittenText: string
}

// ── Workflow-level model settings ──────────────────────────────────────────
export interface WorkflowModelSettings {
  projectId: string
  defaultTextModel: string | null
  defaultImageModel: string | null
  defaultVideoModel: string | null
  overrides: Record<string, string>
}

export interface WorkflowModelSettingsUpdateRequest {
  defaultTextModel?: string | null
  defaultImageModel?: string | null
  defaultVideoModel?: string | null
  overrides?: Record<string, string>
}

/** All stable keys used in workflowModelOverrides */
export const WorkflowModelKey = {
  SCRIPT_REFINE: 'script_refine',
  SCRIPT_APPEND: 'script_append',
  SCRIPT_REWRITE: 'script_rewrite',
  OPTIMIZE_SCENES: 'optimize_scenes',
  OPTIMIZE_CHARACTERS: 'optimize_characters',
  OPTIMIZE_PROPS: 'optimize_props',
  ART_DIRECTION: 'art_direction',
  CHARACTER_VISUAL_PROMPT: 'character_visual_prompt',
  KEYFRAME_IMAGE: 'keyframe_image',
  TURNAROUND_PLAN: 'turnaround_plan',
  TURNAROUND_IMAGE: 'turnaround_image',
  STORYBOARD_PLAN: 'storyboard_plan',
  STORYBOARD_IMAGE: 'storyboard_image',
  GROUP_SCENE_IMAGE: 'group_scene_image',
  THREE_VIEW_IMAGE: 'three_view_image',
  SHOT_VISUAL_PROMPT: 'shot_visual_prompt',
  VIDEO_GENERATION: 'video_generation',
} as const

export type WorkflowModelKeyType = typeof WorkflowModelKey[keyof typeof WorkflowModelKey]
