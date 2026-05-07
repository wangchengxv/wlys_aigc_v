export type GenerateMode = 'text' | 'image' | 'both' | 'video'

/** 全局设定：视觉风格来源（与 React 全局设定页一致，供工作台视频生成解析） */
export type GlobalVisualStyleMode = 'preset' | 'custom'

/** 系统视频风格库单条 */
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
  createdAt: string
  updatedAt: string
}

export interface ConnectionConfigCreateRequest {
  name: string
  provider: string
  baseUrl: string
  apiKey: string
  enabled: boolean
}

export interface ConnectionConfigUpdateRequest {
  name?: string
  provider?: string
  baseUrl?: string
  apiKey?: string
  enabled?: boolean
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

export interface RouterApiKey {
  id: string
  name: string
  key?: string | null
  maskedKey: string
  active: boolean
  createdAt: string
  lastUsedAt?: string | null
}

export interface TimeScheduleSlot {
  start: string
  end: string
  connectionId: string
}

export interface RouterRoutingConfig {
  strategy: string
  priorityConnectionIds: string[]
  failoverEnabled: boolean
  failoverTimeoutSeconds: number
  timeSchedule: TimeScheduleSlot[]
}

export interface RouterRequestLog {
  id: string
  timestamp: string
  routerApiKeyId?: string | null
  connectionId?: string | null
  connectionName?: string | null
  provider?: string | null
  model?: string | null
  requestFormat?: string | null
  status: string
  durationMs: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
  errorMessage?: string | null
}

export interface RouterStats {
  requestsToday: number
  requestsWeek: number
  requestsMonth: number
  tokensToday: number
  tokensWeek: number
  tokensMonth: number
  totalRequests: number
  totalTokens: number
}

export interface RouterLogPage {
  list: RouterRequestLog[]
  total: number
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

export interface ExtractedAsset {
  assetId: string
  projectId: string
  assetType: AssetType
  name: string
  description: string
  sourceShotId?: string | null
  tags: string[]
  promptDraft: string
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
  characterRefs: string[]
  backgroundRefs: string[]
  propRefs: string[]
  keyframeRefs: string[]
  status: string
  createdAt: string
  updatedAt: string
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

export interface ScriptProjectAggregate {
  project: ScriptProject
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
}
