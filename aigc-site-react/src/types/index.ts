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

export type StyleTemplateScope = 'SYSTEM' | 'COURSE' | 'PERSONAL'

export interface StyleTemplate {
  templateId: string
  scope: StyleTemplateScope
  name: string
  category?: string | null
  traits?: string | null
  fullPrompt: string
  styleKey?: string | null
  ownerId?: string | null
  ownerName?: string | null
  orgUnitId?: string | null
  courseId?: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface StyleTemplateCreateRequest {
  scope?: StyleTemplateScope
  name: string
  category?: string
  traits?: string
  fullPrompt: string
  styleKey?: string
  courseId?: string
}

export interface StyleTemplateUpdateRequest {
  name?: string
  category?: string
  traits?: string
  fullPrompt?: string
  styleKey?: string
  enabled?: boolean
}

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface CurrentUser {
  userId: string
  username: string
  displayName: string
  role: UserRole
  orgUnitId?: string | null
  classroomId?: string | null
  enabled: boolean
  permissions?: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: CurrentUser
}

export interface SocialAuthUrlResponse {
  provider: string
  authUrl: string
}

export interface SocialLinkItem {
  provider: string
  providerUserId: string
  linkedAt: string | null
}

export type OrgUnitType = 'ORGANIZATION' | 'CLASSROOM'

export interface OrgUnit {
  unitId: string
  name: string
  code?: string | null
  type: OrgUnitType
  parentUnitId?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface OrgUnitCreateRequest {
  name: string
  code?: string
  type: OrgUnitType
  parentUnitId?: string
}

export interface AdminUser {
  userId: string
  username: string
  displayName: string
  role: UserRole
  orgUnitId?: string | null
  classroomId?: string | null
  enabled: boolean
  locked?: boolean
  lockReason?: string | null
  lockedAt?: string | null
  failedLoginCount?: number
  lastLoginAt?: string | null
  lastLoginIp?: string | null
  passwordUpdatedAt?: string | null
  forcePasswordChange?: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export interface AdminUserCreateRequest {
  username: string
  password: string
  displayName: string
  role: UserRole
  orgUnitId?: string
  classroomId?: string
  enabled?: boolean
}

export interface AdminUserUpdateRequest {
  displayName?: string
  role?: UserRole
  orgUnitId?: string
  classroomId?: string
  enabled?: boolean
  password?: string
}

export interface AdminUserLockUpdateRequest {
  locked: boolean
  reason?: string
}

export interface AdminUserPasswordResetRequest {
  password: string
  forcePasswordChange?: boolean
}

export interface AdminUserBatchOperationResponse {
  total: number
  success: number
  failed: number
  failedUserIds: string[]
}

export interface AdminUserImportErrorItem {
  rowNumber: number
  username: string
  message: string
}

export interface AdminUserImportResult {
  taskId: string
  totalRows: number
  successRows: number
  failedRows: number
  errors: AdminUserImportErrorItem[]
}

export interface AdminUserImportTask {
  taskId: string
  status: string
  sourceFileName?: string | null
  operatorUserId?: string | null
  operatorUserName?: string | null
  totalRows: number
  successRows: number
  failedRows: number
  createdAt?: string | null
  finishedAt?: string | null
  errors: AdminUserImportErrorItem[]
}

export interface AdminUserBatchStatsItem {
  action: string
  operatorUserId?: string | null
  operatorUserName?: string | null
  createdAt?: string | null
  total: number
  success: number
  failed: number
  failedUserIds: string[]
}

export interface AdminUserBatchStatsResponse {
  items: AdminUserBatchStatsItem[]
  totalRequested: number
  totalSuccess: number
  totalFailed: number
}

export interface PagedResult<T> {
  list: T[]
  total: number
}

export type StorageProvider = 'LOCAL'

export interface MediaResource {
  fileId: string
  projectId?: string | null
  fileName?: string | null
  relativePath?: string | null
  storageProvider?: StorageProvider | null
  bucketName?: string | null
  objectKey?: string | null
  publicUrl?: string | null
  mediaType?: string | null
  sizeBytes: number
  createdAt?: string | null
}

export interface AuditLogRecord {
  id: number
  entityType: string
  entityId: string
  action: string
  actorUserId?: string | null
  actorUserName?: string | null
  orgUnitId?: string | null
  courseId?: string | null
  detailsJson?: string | null
  createdAt: string
}

export interface OperationsDashboardMetric {
  key: string
  label: string
  value: number
  entityType?: string | null
  link?: string | null
  summary?: string | null
}

export interface OperationsDashboardStatusBucket {
  key: string
  label: string
  count: number
  entityType?: string | null
  link?: string | null
  summary?: string | null
}

export interface OperationsDashboardActivity {
  key: string
  action: string
  label: string
  summary: string
  entityType?: string | null
  entityId?: string | null
  link?: string | null
  occurredAt?: string | null
}

export interface OperationsDashboardResponse {
  generatedAt: string
  overviewCards: OperationsDashboardMetric[]
  statusDistribution: OperationsDashboardStatusBucket[]
  recentActivities: OperationsDashboardActivity[]
}

export type AssignmentStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'
export type SubmissionStatus = 'SUBMITTED' | 'RETURNED' | 'REVIEWED'

export interface TeachingCourse {
  courseId: string
  name: string
  code?: string | null
  description?: string | null
  ownerId?: string | null
  ownerName?: string | null
  orgUnitId?: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface TeachingAssignment {
  assignmentId: string
  courseId: string
  title: string
  brief?: string | null
  styleTemplateId?: string | null
  aspectRatio?: string | null
  targetDuration?: number | null
  language?: string | null
  dueAt?: string | null
  ownerId?: string | null
  ownerName?: string | null
  status: AssignmentStatus
  createdAt: string
  updatedAt: string
}

export interface AssignmentSubmission {
  submissionId: string
  assignmentId: string
  courseId: string
  projectId: string
  studentUserId: string
  studentUserName?: string | null
  note?: string | null
  status: SubmissionStatus
  score?: number | null
  reviewComment?: string | null
  submittedAt?: string | null
  reviewedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface ReviewRecord {
  reviewId: string
  submissionId: string
  assignmentId: string
  reviewerUserId: string
  reviewerUserName?: string | null
  status: SubmissionStatus
  score?: number | null
  comment?: string | null
  createdAt: string
}

export interface CourseCreateRequest {
  name: string
  code?: string
  description?: string
}

export interface TeachingCourseArchiveRequest {
  archived?: boolean
}

export interface AssignmentCreateRequest {
  title: string
  brief?: string
  styleTemplateId?: string
  aspectRatio?: string
  targetDuration?: number
  language?: string
  dueAt: string
}

export interface TeachingAssignmentStatusUpdateRequest {
  status: AssignmentStatus
}

export interface SubmissionCreateRequest {
  projectId: string
  note?: string
}

export interface SubmissionReviewRequest {
  status: SubmissionStatus
  score?: number
  comment?: string
}

/** Vidu POST //vidu/vidu/ent/v2/img2video optional fields（与 model/images/prompt 互斥，由服务端合并） */
export interface VideoViduOptions {
  duration?: number
  seed?: number
  resolution?: string
  movement_amplitude?: 'auto' | 'small' | 'medium' | 'large' | string
  payload?: string
  off_peak?: boolean
  watermark?: boolean
  wm_position?: number
  wm_url?: string
  meta_data?: string
  callback_url?: string
  audio?: boolean
  audio_type?: 'all' | 'speech_only' | 'sound_effect_only' | string
  voice_id?: string
  is_rec?: boolean
  bgm?: boolean
}

export type ImageAdvancedCapability = 'vidu_reference2image' | 'kling_multi_reference' | 'outpaint' | 'omni'

export interface GenerateAdvancedImageReference2ImageRequest {
  referenceImageUrl?: string
  images?: string[]
}

export interface GenerateAdvancedImageKlingMultiReferenceRequest {
  referenceImageUrls?: string[]
  images?: string[]
}

export interface GenerateAdvancedImageOutpaintRequest {
  sourceImageUrl?: string
  image?: string
  top?: number
  right?: number
  bottom?: number
  left?: number
}

export interface GenerateAdvancedImageOmniRequest {
  sourceImageUrl?: string
  image?: string
  mode?: string
  subjectPrompt?: string
}

export interface GenerateAdvancedImageExtraRequest {
  capability?: ImageAdvancedCapability
  reference2image?: GenerateAdvancedImageReference2ImageRequest
  klingMultiReference?: GenerateAdvancedImageKlingMultiReferenceRequest
  outpaint?: GenerateAdvancedImageOutpaintRequest
  omni?: GenerateAdvancedImageOmniRequest
  [key: string]: unknown
}

export interface GenerateAdvancedImageRequest {
  /** 单参考图入口，兼容 reference2image 等场景 */
  referenceImageUrl?: string
  /** 统一图片高级能力入口 */
  extra?: GenerateAdvancedImageExtraRequest
}

export interface GenerateAdvancedVideoRequest {
  /** 统一视频参考图入口 */
  referenceImageUrl?: string
  /** Vidu 图生视频高级参数 */
  viduOptions?: VideoViduOptions
  /** OneLink 豆包 Seedance 视频扩展字段 */
  extra?: GenerateAdvancedVideoOneLinkSeedanceExtraRequest | Record<string, unknown>
}

export interface GenerateAdvancedVideoOneLinkSeedanceExtraRequest {
  text?: string
  referenceImageUrls?: string[]
  referenceVideoUrls?: string[]
  referenceAudioUrls?: string[]
  generate_audio?: boolean
  ratio?: string
  duration?: number
  watermark?: boolean
}

export interface GenerateAdvancedMediaRequest {
  image?: GenerateAdvancedImageRequest
  video?: GenerateAdvancedVideoRequest
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
  /** 统一高级媒体请求结构 */
  advancedMedia?: GenerateAdvancedMediaRequest
  /** Moark 等图生视频：参考图 HTTP(S) URL */
  videoReferenceImageUrl?: string
  /** Vidu 图生视频可选参数 */
  videoViduOptions?: VideoViduOptions
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

export interface VideoModelOptionDetail {
  modelName: string
  displayName: string
  provider: string
  capability: string
  enabled: boolean
  connectionEnabled: boolean
}

export interface VideoModelOptions {
  defaultModel: string
  options: string[]
  details?: VideoModelOptionDetail[]
}

export interface ReversePromptModelOptionDetail {
  modelName: string
  displayName: string
  provider: string
  capability: string
  enabled: boolean
  connectionEnabled: boolean
}

export interface ReversePromptModelOptions {
  defaultModel: string | null
  options: string[]
  details?: ReversePromptModelOptionDetail[]
}

export interface ReversePromptRequest {
  image: string
  model?: string
}

export interface ReversePromptResponse {
  model: string
  positivePrompt: string
  negativePrompt: string
  style: string
  lighting: string
  composition: string
  camera: string
  colorTone: string
  parameters?: Record<string, string>
  rawText: string
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
  id: string
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

export type ProviderCatalogAuthMode = 'BEARER' | 'X_API_KEY' | 'API_KEY_HEADER' | 'TOKEN' | 'NONE'

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
  | 'DUBBING_GENERATING'
  | 'LIP_SYNC_GENERATING'
  | 'VIDEO_EDITING_RENDERING'
  | 'FINAL_COMPOSITION_GENERATING'
  | 'EXPORT_PACKAGE_GENERATING'
  | 'VIDEO_READY'
  | 'DUBBING_READY'
  | 'LIP_SYNC_READY'
  | 'VIDEO_EDITING_READY'
  | 'FINAL_COMPOSITION_READY'
  | 'EXPORT_PACKAGE_READY'
  | 'COMPLETED'
  | 'PARTIAL_FAILED'
  | 'FAILED'

export type ContentReviewStatus = 'NOT_SUBMITTED' | 'PENDING' | 'APPROVED' | 'REJECTED'

export type AssetType = 'CHARACTER' | 'BACKGROUND' | 'PROP'
export type AssetStatus = 'PENDING' | 'EXTRACTED' | 'KEYFRAME_GENERATING' | 'KEYFRAME_READY' | 'CONFIRMED' | 'FAILED'
export type SegmentTaskStatus = 'PENDING' | 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED'
export type PipelineRunStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_FAILED' | 'FAILED'
export type PipelineType =
  | 'REFINE'
  | 'ASSET_EXTRACTION'
  | 'KEYFRAME_GENERATION'
  | 'SHOT_SPLIT'
  | 'VIDEO_GENERATION'
  | 'DUBBING'
  | 'LIP_SYNC'
  | 'VIDEO_EDITING'
  | 'FINAL_COMPOSITION'
  | 'EXPORT_PACKAGE'

export interface ScriptProject {
  projectId: string
  ownerId?: string | null
  ownerName?: string | null
  orgUnitId?: string | null
  courseId?: string | null
  name: string
  status: ProjectStatus
  sourceType: string
  originalScriptFileId?: string | null
  refinedScriptFileId?: string | null
  refinedScriptJsonFileId?: string | null
  uploadedSourceFileId?: string | null
  scriptSummary?: string | null
  visualStyle: string
  styleTemplateId?: string | null
  aspectRatio: string
  targetDuration: number
  language: string
  explicitTextModel?: string | null
  explicitImageModel?: string | null
  explicitVideoModel?: string | null
  explicitTtsModel?: string | null
  dubbingVoice?: string | null
  dubbingLanguage?: string | null
  dubbingSpeed?: number | null
  /** B-1 全局美术指导 JSON */
  artDirectionJson?: string | null
  contentReviewStatus?: ContentReviewStatus
  currentReviewId?: string | null
  latestReviewComment?: string | null
  reviewResubmitCount?: number | null
  reviewSubmittedAt?: string | null
  reviewedAt?: string | null
  reviewerUserId?: string | null
  reviewerUserName?: string | null
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

export interface DubbingTask {
  dubbingTaskId: string
  projectId: string
  shotId: string
  requestPayloadFileId?: string | null
  resultAudioFileId?: string | null
  providerTaskId?: string | null
  modelName?: string | null
  language?: string | null
  voiceName?: string | null
  speechRate?: number | null
  inputText: string
  status: SegmentTaskStatus
  retryCount: number
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
}

export interface LipSyncTask {
  lipSyncTaskId: string
  projectId: string
  shotId: string
  sourceVideoFileId?: string | null
  sourceAudioFileId?: string | null
  requestPayloadFileId?: string | null
  resultVideoFileId?: string | null
  providerTaskId?: string | null
  modelName?: string | null
  status: SegmentTaskStatus
  retryCount: number
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
}

export type VideoEditingSourceType = 'LIP_SYNC' | 'VIDEO'
export type VideoEditingTransitionMode = 'CUT' | 'FADE' | 'DIP_TO_BLACK'
export type VideoEditingRenderType = 'PREVIEW' | 'PUBLISH'

export interface VideoEditingSourceOption {
  sourceType: VideoEditingSourceType
  sourceFileId: string
  sourceTaskId?: string | null
  label?: string | null
  durationSeconds?: number | null
  available?: boolean
}

export interface VideoEditingDraftSegment {
  segmentId: string
  shotId: string
  sequenceNo: number
  enabled: boolean
  sourceType: VideoEditingSourceType
  sourceFileId: string
  sourceTaskId?: string | null
  sourceDurationSeconds?: number | null
  trimInSeconds: number
  trimOutSeconds: number
  transitionMode: VideoEditingTransitionMode
  transitionDurationSeconds?: number | null
  notes?: string | null
  extension?: Record<string, unknown> | null
  availableSources?: VideoEditingSourceOption[]
}

export interface VideoEditingDraftSegmentInput {
  segmentId?: string | null
  shotId: string
  sequenceNo: number
  enabled: boolean
  sourceType: VideoEditingSourceType
  sourceFileId: string
  sourceTaskId?: string | null
  trimInSeconds: number
  trimOutSeconds: number
  transitionMode: VideoEditingTransitionMode
  transitionDurationSeconds?: number | null
  notes?: string | null
  extension?: Record<string, unknown> | null
}

export interface VideoEditingRenderSegment {
  segmentId: string
  shotId: string
  sequenceNo: number
  sourceType: VideoEditingSourceType
  sourceFileId: string
  sourceTaskId?: string | null
  trimInSeconds: number
  trimOutSeconds: number
  transitionMode: VideoEditingTransitionMode
  transitionDurationSeconds?: number | null
}

export interface VideoEditingRenderTask {
  renderTaskId: string
  projectId: string
  draftId?: string | null
  draftVersion: number
  renderType: VideoEditingRenderType
  inputSegments: VideoEditingRenderSegment[]
  requestPayloadFileId?: string | null
  resultVideoFileId?: string | null
  providerTaskId?: string | null
  modelName?: string | null
  status: SegmentTaskStatus
  retryCount: number
  published?: boolean
  publishedAt?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
}

export interface VideoEditingDraft {
  draftId: string
  projectId: string
  version: number
  publishedVersion?: number | null
  hasPublishedResult?: boolean
  hasUnpublishedChanges: boolean
  lastSavedAt?: string | null
  publishedAt?: string | null
  publishedRenderTaskId?: string | null
  latestPreviewRenderTaskId?: string | null
  latestPublishTaskId?: string | null
  publishedVideoFileId?: string | null
  extension?: Record<string, unknown> | null
  segments: VideoEditingDraftSegment[]
  renderTasks: VideoEditingRenderTask[]
}

export interface VideoEditingSaveDraftRequest {
  version?: number | null
  extension?: Record<string, unknown> | null
  segments: VideoEditingDraftSegmentInput[]
}

export interface VideoEditingRenderRequest {
  draftVersion?: number | null
}

export interface VideoEditingPublishRequest {
  draftVersion?: number | null
  renderTaskId?: string | null
}

export interface FinalCompositionInputSegment {
  shotId: string
  sequenceNo: number
  sourceType: 'LIP_SYNC' | 'VIDEO'
  sourceFileId: string
  sourceTaskId?: string | null
  durationSeconds?: number | null
}

export interface FinalCompositionTask {
  finalCompositionTaskId: string
  projectId: string
  inputSegments: FinalCompositionInputSegment[]
  requestPayloadFileId?: string | null
  resultVideoFileId?: string | null
  providerTaskId?: string | null
  modelName?: string | null
  status: SegmentTaskStatus
  retryCount: number
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
}

export interface ExportPackageTask {
  exportPackageTaskId: string
  projectId: string
  sourceVideoEditingRenderTaskId?: string | null
  sourceVideoEditingPublishedAt?: string | null
  sourceFinalCompositionTaskId?: string | null
  sourceFinalVideoFileId?: string | null
  manifestFileId?: string | null
  resultArchiveFileId?: string | null
  status: SegmentTaskStatus
  retryCount: number
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
  archiveStorageProvider?: string | null
  archiveBucketName?: string | null
  archiveObjectKey?: string | null
  archivePublicUrl?: string | null
  manifestStorageProvider?: string | null
  manifestBucketName?: string | null
  manifestObjectKey?: string | null
  manifestPublicUrl?: string | null
}

export interface ContentReviewRecord {
  reviewId: string
  projectId: string
  status: ContentReviewStatus
  submitterUserId?: string | null
  submitterUserName?: string | null
  submissionComment?: string | null
  reviewerUserId?: string | null
  reviewerUserName?: string | null
  reviewComment?: string | null
  resubmitCount?: number | null
  submittedAt?: string | null
  reviewedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ContentReviewSubmitRequest {
  comment?: string | null
}

export interface ContentReviewDecisionRequest {
  comment?: string | null
}

export interface ContentReviewStatusResponse {
  projectId: string
  status: ContentReviewStatus
  exportPackageReady: boolean
  currentReviewId?: string | null
  resubmitCount?: number | null
  latestReviewComment?: string | null
  reviewSubmittedAt?: string | null
  reviewedAt?: string | null
  reviewerUserId?: string | null
  reviewerUserName?: string | null
  canSubmit: boolean
  canProcess: boolean
  records: ContentReviewRecord[]
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
  contentReviewRecords?: ContentReviewRecord[]
  revisions?: ScriptRevision[]
  documents: ScriptDocumentVersion[]
  files: StoredFileRecord[]
  assets: ExtractedAsset[]
  keyframes: KeyframeRecord[]
  shots: StoryboardShot[]
  videoTasks: VideoSegmentTask[]
  dubbingTasks: DubbingTask[]
  lipSyncTasks: LipSyncTask[]
  videoEditingDraft?: VideoEditingDraft | null
  finalCompositionTasks?: FinalCompositionTask[]
  exportPackageTasks?: ExportPackageTask[]
  pipelineRuns: PipelineRun[]
}

export interface ScriptProjectSummary {
  projectId: string
  ownerId?: string | null
  ownerName?: string | null
  orgUnitId?: string | null
  courseId?: string | null
  name: string
  status: ProjectStatus
  scriptSummary?: string | null
  visualStyle: string
  styleTemplateId?: string | null
  aspectRatio: string
  targetDuration: number
  contentReviewStatus?: ContentReviewStatus
  reviewResubmitCount?: number | null
  latestReviewComment?: string | null
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
  videoTaskCount?: number
  dubbingTaskCount?: number
  dubbingSuccessCount?: number
  dubbingFailedCount?: number
  dubbingRunningCount?: number
  dubbingQueuedCount?: number
  dubbingPendingCount?: number
  lipSyncTaskCount?: number
  lipSyncSuccessCount?: number
  lipSyncFailedCount?: number
  lipSyncRunningCount?: number
  lipSyncQueuedCount?: number
  lipSyncPendingCount?: number
  videoEditingDraftVersion?: number
  videoEditingRenderTaskCount?: number
  videoEditingSuccessCount?: number
  videoEditingFailedCount?: number
  videoEditingRunningCount?: number
  videoEditingQueuedCount?: number
  videoEditingPendingCount?: number
  videoEditingPublishedAt?: string | null
  finalCompositionTaskCount?: number
  finalCompositionSuccessCount?: number
  finalCompositionFailedCount?: number
  finalCompositionRunningCount?: number
  finalCompositionQueuedCount?: number
  finalCompositionPendingCount?: number
  exportPackageTaskCount?: number
  exportPackageSuccessCount?: number
  exportPackageFailedCount?: number
  exportPackageRunningCount?: number
  exportPackageQueuedCount?: number
  exportPackagePendingCount?: number
  contentReviewStatus?: ContentReviewStatus
  reviewResubmitCount?: number
  currentReviewId?: string | null
  latestReviewComment?: string | null
  videoReady?: boolean
  dubbingReady?: boolean
  lipSyncReady?: boolean
  videoEditingReady?: boolean
  finalCompositionReady?: boolean
  exportPackageReady?: boolean
}

export interface ScriptProjectCreateRequest {
  name: string
  sourceText: string
  visualStyle?: string
  styleTemplateId?: string
  aspectRatio?: string
  targetDuration?: number
  language?: string
  courseId?: string
  explicitTextModel?: string
  explicitImageModel?: string
  explicitVideoModel?: string
}

export interface ScriptProjectUploadRequest {
  name: string
  file: File
  visualStyle?: string
  styleTemplateId?: string
  aspectRatio?: string
  targetDuration?: number
  language?: string
  courseId?: string
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
  defaultTtsModel: string | null
  dubbingVoice: string | null
  dubbingLanguage: string | null
  dubbingSpeed: number | null
  overrides: Record<string, string>
}

export interface WorkflowModelSettingsUpdateRequest {
  defaultTextModel?: string | null
  defaultImageModel?: string | null
  defaultVideoModel?: string | null
  defaultTtsModel?: string | null
  dubbingVoice?: string | null
  dubbingLanguage?: string | null
  dubbingSpeed?: number | null
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
  TTS_DUBBING: 'tts_dubbing',
} as const

export type WorkflowModelKeyType = typeof WorkflowModelKey[keyof typeof WorkflowModelKey]
