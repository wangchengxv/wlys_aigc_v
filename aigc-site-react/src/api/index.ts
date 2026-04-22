import axios, { AxiosHeaders, isAxiosError } from 'axios'
import { FALLBACK_STYLE_TEMPLATES } from '@/data/videoStylePresets'
import {
  normalizePipelineStatus,
  normalizeScriptProjectAggregate,
  normalizeVideoEditingDraftResponse,
  normalizeVideoEditingRenderTask,
  toVideoEditingPublishPayload,
  toVideoEditingSaveDraftPayload,
} from '@/lib/scriptProject/videoEditingContract'
import type {
  AssignmentCreateRequest,
  AssignmentSubmission,
  ArtDirectionResponse,
  AuditLogRecord,
  BatchModelsImportRequest,
  BatchVisualPromptResponse,
  ApplyStoryboardFirstFrameRequest,
  AdminUser,
  AdminUserBatchOperationResponse,
  AdminUserBatchStatsResponse,
  AdminUserCreateRequest,
  AdminUserImportResult,
  AdminUserImportTask,
  AdminUserLockUpdateRequest,
  AdminUserPasswordResetRequest,
  AdminUserUpdateRequest,
  CurrentUser,
  TeachingAssignmentStatusUpdateRequest,
  TeachingCourseArchiveRequest,
  AppendScriptPreviewRequest,
  AppendScriptPreviewResponse,
  AssetGenerationHistoryItem,
  AssetHistoryType,
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ConnectionConfigUpdateRequest,
  ConnectionTestResponse,
  ContentReviewDecisionRequest,
  ContentReviewStatusResponse,
  ContentReviewSubmitRequest,
  CourseCreateRequest,
  GenerateGroupSceneRequest,
  GroupSceneResponse,
  ExtractedAsset,
  GenerateRequest,
  GenerateResponse,
  HistoryQuery,
  ImageModelOptions,
  KeyframeRecord,
  DubbingTask,
  ExportPackageTask,
  FinalCompositionTask,
  LipSyncTask,
  ModelConfig,
  ModelConfigCreateRequest,
  ModelConfigUpdateRequest,
  ModelProbeResponse,
  PagedTasks,
  PipelineStatus,
  LoginRequest,
  LoginResponse,
  PromptTemplateCatalogItem,
  PagedResult,
  PresetModelListResponse,
  SocialAuthUrlResponse,
  SocialLinkItem,
  OrgUnit,
  OrgUnitCreateRequest,
  MediaResource,
  OperationsDashboardResponse,
  ProviderCatalogListResponse,
  QuickConnectionRequest,
  ReviewRecord,
  RewriteScriptApplyRequest,
  RewriteScriptPreviewRequest,
  RewriteScriptPreviewResponse,
  ScriptDocumentPayload,
  ScriptProjectAggregate,
  ScriptProjectCreateRequest,
  ScriptProjectSummary,
  ScriptProjectUploadRequest,
  ScriptRevision,
  ShotVisualPromptResponse,
  StoryboardFirstFrameResponse,
  StoryboardImageResponse,
  StoryboardPanelCropResponse,
  StoryboardPlanResponse,
  StoryboardRewriteRequest,
  StyleTemplate,
  StyleTemplateCreateRequest,
  StyleTemplateUpdateRequest,
  SubmissionCreateRequest,
  SubmissionReviewRequest,
  SubmissionStatus,
  TeachingAssignment,
  TeachingCourse,
  ThreeViewResponse,
  StoryboardShot,
  TurnaroundImageResponse,
  TurnaroundPlanResponse,
  UpdateAssetRequest,
  UpdateShotRequest,
  UpdateScriptRequest,
  VisualPromptResponse,
  VideoEditingDraft,
  VideoEditingPublishRequest,
  VideoEditingRenderRequest,
  VideoEditingSaveDraftRequest,
  VideoEditingRenderTask,
  VideoSegmentTask,
  VideoModelOptions,
  WorkflowModelSettings,
  WorkflowModelSettingsUpdateRequest,
} from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const USE_MOCK = !API_BASE_URL
const STORAGE_KEY = 'aigc_tasks_v1'
const STYLE_TEMPLATE_MOCK_KEY = 'aigc_style_templates_v1'
const AUTH_ACCESS_TOKEN_KEY = 'aigc_access_token'
const DEFAULT_MODEL = 'doubao-seedream-5-0-260128'
const DEFAULT_VIDEO_MODEL = 'doubao-seedance-1-5-pro-251215'
const CLIENT_USER_ID_KEY = 'aigc_client_user_id'
const CLIENT_USER_NAME_KEY = 'aigc_client_user_name'
const CLIENT_ORG_UNIT_ID_KEY = 'aigc_client_org_unit_id'

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T | null
}

function tryParseJsonText(value: string): unknown {
  const text = value.trim()
  if (!text) return value
  try {
    return JSON.parse(text)
  } catch {
    const fenceMatch = text.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i)
    if (fenceMatch?.[1]) {
      const inner = fenceMatch[1].trim()
      try {
        return JSON.parse(inner)
      } catch {
        return value
      }
    }
    return value
  }
}

function normalizeScriptDocumentPayload(raw: unknown): ScriptDocumentPayload {
  let payload = raw
  if (typeof payload === 'string') {
    payload = tryParseJsonText(payload)
  }
  if (!payload || typeof payload !== 'object') {
    throw new Error('剧本解析失败：返回数据格式不正确')
  }

  const data = payload as Record<string, unknown>
  const projectId = String(data.projectId ?? '')
  const originalText = String(data.originalText ?? '')
  const refinedMarkdown = String(data.refinedMarkdown ?? '')

  let structuredScript: unknown = data.structuredScript ?? {}
  if (typeof structuredScript === 'string') {
    structuredScript = tryParseJsonText(structuredScript)
  }
  if (!structuredScript || typeof structuredScript !== 'object') {
    structuredScript = {}
  }

  const documents = Array.isArray(data.documents) ? (data.documents as ScriptDocumentPayload['documents']) : []

  return {
    projectId,
    originalText,
    refinedMarkdown,
    structuredScript: structuredScript as Record<string, unknown>,
    documents,
  }
}

function unwrapApiData<T>(payload: ApiEnvelope<T>, fallbackMessage: string): T {
  if (!payload || payload.code !== 200 || payload.data == null) {
    throw new Error(payload?.message || fallbackMessage)
  }
  return payload.data
}

function unwrapApiVoid(payload: ApiEnvelope<unknown> | undefined | null, fallbackMessage: string): void {
  if (payload == null) return
  if (typeof payload !== 'object' || Array.isArray(payload)) return
  const p = payload as ApiEnvelope<unknown>
  if (p.code !== 200) {
    throw new Error(p.message || fallbackMessage)
  }
}

function enhanceAxiosError(e: unknown): Error {
  if (isAxiosError(e)) {
    const body = e.response?.data
    if (body && typeof body === 'object') {
      const message = (body as { message?: string }).message
      if (typeof message === 'string' && message) {
        return new Error(message)
      }
      const nestedError = (body as { error?: { message?: string } }).error?.message
      if (typeof nestedError === 'string' && nestedError) {
        return new Error(nestedError)
      }
    }
    if (e.code === 'ECONNABORTED') {
      return new Error('请求超时，请稍后重试')
    }
    if (!e.response) {
      return new Error('网络异常，请检查服务是否可达')
    }
    if (e.response.status === 401) {
      return new Error('登录态已失效或鉴权失败，请检查访问令牌')
    }
    if (e.response.status === 403) {
      return new Error('当前账号无权限执行该操作')
    }
    if (e.response.status >= 500) {
      return new Error('服务端异常，请稍后重试')
    }
  }
  return e instanceof Error ? e : new Error(String(e))
}

const http = axios.create({
  baseURL: API_BASE_URL || undefined,
  timeout: 1300000,
})

function getOrCreateClientUserId() {
  const existing = localStorage.getItem(CLIENT_USER_ID_KEY)
  if (existing && existing.trim()) {
    return existing
  }
  const created = `u_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
  localStorage.setItem(CLIENT_USER_ID_KEY, created)
  return created
}

function getClientProfile() {
  const userId = getOrCreateClientUserId()
  const existingName = localStorage.getItem(CLIENT_USER_NAME_KEY)?.trim()
  const existingOrg = localStorage.getItem(CLIENT_ORG_UNIT_ID_KEY)?.trim()
  const userName = existingName || `创作者-${userId.slice(-4)}`
  const orgUnitId = existingOrg || 'default-org'
  if (!existingName) localStorage.setItem(CLIENT_USER_NAME_KEY, userName)
  if (!existingOrg) localStorage.setItem(CLIENT_ORG_UNIT_ID_KEY, orgUnitId)
  return { userId, userName, orgUnitId }
}

function courseHeaderConfig(courseId?: string | null) {
  const normalized = courseId?.trim()
  if (!normalized) {
    return undefined
  }
  return {
    headers: {
      'x-course-id': normalized,
    },
  }
}

export function getStoredAccessToken() {
  return localStorage.getItem(AUTH_ACCESS_TOKEN_KEY)?.trim() || ''
}

export function setStoredAccessToken(token: string | null) {
  if (!token || !token.trim()) {
    localStorage.removeItem(AUTH_ACCESS_TOKEN_KEY)
    return
  }
  localStorage.setItem(AUTH_ACCESS_TOKEN_KEY, token.trim())
}

export function clearStoredAccessToken() {
  localStorage.removeItem(AUTH_ACCESS_TOKEN_KEY)
}

http.interceptors.request.use((config) => {
  if (!USE_MOCK) {
    const token = getStoredAccessToken()
    const headers = AxiosHeaders.from(config.headers ?? {})
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
      headers.set('x-aigc-token', token)
    }
    config.headers = headers
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(enhanceAxiosError(error)),
)

function getMockTasks(): GenerateResponse[] {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as GenerateResponse[]
  } catch {
    return []
  }
}

function saveMockTasks(tasks: GenerateResponse[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks))
}

function getMockStyleTemplates(): StyleTemplate[] {
  const base = [...FALLBACK_STYLE_TEMPLATES]
  const raw = localStorage.getItem(STYLE_TEMPLATE_MOCK_KEY)
  if (!raw) return base
  try {
    const parsed = JSON.parse(raw) as StyleTemplate[]
    return [...base, ...parsed]
  } catch {
    return base
  }
}

function saveMockStyleTemplates(templates: StyleTemplate[]) {
  const personalOnly = templates.filter((item) => item.scope !== 'SYSTEM')
  localStorage.setItem(STYLE_TEMPLATE_MOCK_KEY, JSON.stringify(personalOnly))
}

function normalizeTask(task: GenerateResponse): GenerateResponse {
  return {
    ...task,
    textResults: Array.isArray(task.textResults) ? task.textResults : [],
    imageResults: Array.isArray(task.imageResults) ? task.imageResults : [],
    videoResults: Array.isArray(task.videoResults) ? task.videoResults : [],
    persistedImageFileIds: Array.isArray(task.persistedImageFileIds) ? task.persistedImageFileIds : undefined,
    persistedVideoFileIds: Array.isArray(task.persistedVideoFileIds) ? task.persistedVideoFileIds : undefined,
  }
}

function mockText(prompt: string, style: string, len: GenerateRequest['textLength']) {
  const lengthMap = {
    short: '短文案',
    medium: '中等长度文案',
    long: '长文案',
  }
  return [
    `【${style}${lengthMap[len]}】围绕“${prompt}”打造引爆传播点：突出核心卖点、场景化表达、清晰行动引导。`,
    `主题：${prompt}。建议采用“痛点-亮点-福利-行动”结构，先抛问题再给方案，最后加限时权益提升转化。`,
  ]
}

function mockImages(prompt: string, size: string, count: number) {
  return Array.from({ length: count }, (_, idx) => {
    const seed = encodeURIComponent(`${prompt}-${size}-${idx}-${Date.now()}`)
    return `https://picsum.photos/seed/${seed}/1024/768`
  })
}

function mockVideos(prompt: string, count: number) {
  return Array.from({ length: count }, (_, idx) => {
    const seed = encodeURIComponent(`${prompt}-${idx}-${Date.now()}`)
    return `https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4?seed=${seed}`
  })
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function generateContent(req: GenerateRequest): Promise<GenerateResponse> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<GenerateResponse>>('/api/v1/generate', req)
    return normalizeTask(unwrapApiData(data, '生成失败，请稍后重试'))
  }

  const start = performance.now()
  await sleep(1200 + Math.random() * 1500)
  const response: GenerateResponse = {
    taskId: `T${Date.now()}`,
    status: 'SUCCESS',
    textResults: req.mode === 'image' || req.mode === 'video' ? [] : mockText(req.prompt, req.style, req.textLength),
    imageResults: req.mode === 'text' || req.mode === 'video' ? [] : mockImages(req.prompt, req.imageSize, req.count),
    videoResults: req.mode === 'video' ? mockVideos(req.prompt, req.count) : [],
    createdAt: new Date().toISOString(),
    latencyMs: Math.round(performance.now() - start),
    prompt: req.prompt,
    mode: req.mode,
    style: req.style,
    imageModel: req.imageModel || DEFAULT_MODEL,
    videoModel: req.videoModel || DEFAULT_VIDEO_MODEL,
  }
  const list = getMockTasks()
  saveMockTasks([response, ...list])
  return normalizeTask(response)
}

export async function getImageModels(): Promise<ImageModelOptions> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<ImageModelOptions>>('/api/v1/models/image')
    return unwrapApiData(data, '获取模型列表失败')
  }
  return {
    defaultModel: DEFAULT_MODEL,
    options: [DEFAULT_MODEL],
  }
}

export async function getVideoModels(): Promise<VideoModelOptions> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<VideoModelOptions>>('/api/v1/models/video')
    return unwrapApiData(data, '获取视频模型列表失败')
  }
  return {
    defaultModel: DEFAULT_VIDEO_MODEL,
    options: [DEFAULT_VIDEO_MODEL],
    details: [],
  }
}

export async function getHistory(query: HistoryQuery): Promise<PagedTasks> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<PagedTasks>>('/api/v1/history', {
      params: {
        page: query.page,
        pageSize: query.pageSize,
        mode: query.mode === 'all' ? undefined : query.mode,
      },
    })
    const payload = unwrapApiData(data, '获取历史记录失败')
    return {
      ...payload,
      list: payload.list.map(normalizeTask),
    }
  }

  const all = getMockTasks()
  const filtered = query.mode && query.mode !== 'all' ? all.filter((v) => v.mode === query.mode) : all
  const start = (query.page - 1) * query.pageSize
  const end = start + query.pageSize
  return {
    list: filtered.slice(start, end).map(normalizeTask),
    total: filtered.length,
  }
}

export async function getTask(taskId: string): Promise<GenerateResponse | null> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<GenerateResponse>>(`/api/v1/tasks/${taskId}`)
    return normalizeTask(unwrapApiData(data, '获取任务详情失败'))
  }
  const task = getMockTasks().find((t) => t.taskId === taskId)
  return task ? normalizeTask(task) : null
}

export async function healthCheck(): Promise<{ ok: boolean; mode: string }> {
  if (!USE_MOCK) {
    await http.get('/api/v1/health')
    return { ok: true, mode: 'api' }
  }
  await sleep(200)
  return { ok: true, mode: 'mock' }
}

function toMockCurrentUser(): CurrentUser {
  const { userId, userName, orgUnitId } = getClientProfile()
  return {
    userId,
    username: userId,
    displayName: userName,
    role: userId.startsWith('teacher') ? 'TEACHER' : userId.startsWith('admin') ? 'ADMIN' : 'STUDENT',
    orgUnitId,
    enabled: true,
  }
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<LoginResponse>>('/api/v1/auth/login', request)
    const payload = unwrapApiData(data, '登录失败')
    setStoredAccessToken(payload.accessToken)
    return payload
  }
  const user = toMockCurrentUser()
  return {
    accessToken: 'mock-token',
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString(),
    user: {
      ...user,
      username: request.username,
      displayName: request.username || user.displayName,
    },
  }
}

export async function getSocialAuthUrl(provider: string): Promise<SocialAuthUrlResponse> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<SocialAuthUrlResponse>>(`/api/v1/auth/social/${encodeURIComponent(provider)}`)
    return unwrapApiData(data, '获取第三方登录地址失败')
  }
  throw new Error('Mock 模式下不支持第三方登录')
}

export async function socialLoginCallback(provider: string, code: string, state: string): Promise<LoginResponse> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<LoginResponse>>(`/api/v1/auth/social/callback/${encodeURIComponent(provider)}`, {
      params: { code, state },
    })
    const payload = unwrapApiData(data, '第三方登录失败')
    setStoredAccessToken(payload.accessToken)
    return payload
  }
  throw new Error('Mock 模式下不支持第三方登录')
}

export async function getSocialLinks(): Promise<SocialLinkItem[]> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<SocialLinkItem[]>>('/api/v1/auth/social/links')
    return unwrapApiData(data, '获取第三方账号绑定列表失败')
  }
  return []
}

export async function unbindSocialLink(provider: string): Promise<void> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<null>>('/api/v1/auth/social/unbind', { provider })
    unwrapApiVoid(data, '解绑第三方账号失败')
    return
  }
  throw new Error('Mock 模式下不支持第三方解绑')
}

export async function getCurrentUser(): Promise<CurrentUser> {
  if (!USE_MOCK) {
    const storedToken = getStoredAccessToken()
    if (!storedToken) {
      throw new Error('当前未登录')
    }
    const { data } = await http.get<ApiEnvelope<CurrentUser>>('/api/v1/auth/me')
    return unwrapApiData(data, '获取当前用户失败')
  }
  return toMockCurrentUser()
}

export async function logout(): Promise<void> {
  if (!USE_MOCK && getStoredAccessToken()) {
    try {
      await http.post<ApiEnvelope<null>>('/api/v1/auth/logout')
    } catch {
      /* ignore logout failure */
    }
  }
  clearStoredAccessToken()
}

export async function getOrgUnits(): Promise<OrgUnit[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<OrgUnit[]>>('/api/v1/admin/directory/org-units')
  return unwrapApiData(data, '获取组织目录失败')
}

export async function createOrgUnit(payload: OrgUnitCreateRequest): Promise<OrgUnit> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<OrgUnit>>('/api/v1/admin/directory/org-units', payload)
  return unwrapApiData(data, '创建组织或班级失败')
}

export async function getAdminUsers(): Promise<AdminUser[]> {
  requireScriptApi()
  const result = await getAdminUsersPaged({
    page: 1,
    pageSize: 200,
  })
  return result.list
}

export async function getAdminUsersPaged(params: {
  page: number
  pageSize: number
  keyword?: string
  role?: string
  enabled?: boolean
  locked?: boolean
  orgUnitId?: string
  classroomId?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}): Promise<PagedResult<AdminUser>> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<PagedResult<AdminUser>>>('/api/v1/admin/directory/users', {
    params: {
      page: params.page,
      pageSize: params.pageSize,
      keyword: params.keyword,
      role: params.role,
      enabled: params.enabled,
      locked: params.locked,
      orgUnitId: params.orgUnitId,
      classroomId: params.classroomId,
      sortBy: params.sortBy,
      sortOrder: params.sortOrder,
    },
  })
  return unwrapApiData(data, '获取用户目录失败')
}

export async function createAdminUser(payload: AdminUserCreateRequest): Promise<AdminUser> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AdminUser>>('/api/v1/admin/directory/users', payload)
  return unwrapApiData(data, '创建用户失败')
}

export async function updateAdminUser(userId: string, payload: AdminUserUpdateRequest): Promise<AdminUser> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<AdminUser>>(`/api/v1/admin/directory/users/${encodeURIComponent(userId)}`, payload)
  return unwrapApiData(data, '更新用户失败')
}

export async function updateAdminUserStatus(userId: string, enabled: boolean): Promise<AdminUser> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<AdminUser>>(`/api/v1/admin/directory/users/${encodeURIComponent(userId)}/status`, { enabled })
  return unwrapApiData(data, '更新用户状态失败')
}

export async function updateAdminUserLock(userId: string, payload: AdminUserLockUpdateRequest): Promise<AdminUser> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<AdminUser>>(`/api/v1/admin/directory/users/${encodeURIComponent(userId)}/lock`, payload)
  return unwrapApiData(data, '更新账号锁定状态失败')
}

export async function resetAdminUserPassword(userId: string, payload: AdminUserPasswordResetRequest): Promise<AdminUser> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<AdminUser>>(`/api/v1/admin/directory/users/${encodeURIComponent(userId)}/password`, payload)
  return unwrapApiData(data, '重置密码失败')
}

export async function forceLogoutAdminUser(userId: string): Promise<AdminUser> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AdminUser>>(`/api/v1/admin/directory/users/${encodeURIComponent(userId)}/force-logout`)
  return unwrapApiData(data, '强制下线失败')
}

export async function batchUpdateAdminUserStatus(userIds: string[], enabled: boolean): Promise<AdminUserBatchOperationResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AdminUserBatchOperationResponse>>('/api/v1/admin/directory/users/batch/status', {
    userIds,
    enabled,
  })
  return unwrapApiData(data, '批量更新账号状态失败')
}

export async function batchUpdateAdminUserLock(
  userIds: string[],
  locked: boolean,
  reason?: string,
): Promise<AdminUserBatchOperationResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AdminUserBatchOperationResponse>>('/api/v1/admin/directory/users/batch/lock', {
    userIds,
    locked,
    reason,
  })
  return unwrapApiData(data, '批量更新账号锁定状态失败')
}

export async function downloadAdminUserImportTemplate(): Promise<Blob> {
  requireScriptApi()
  const response = await http.get('/api/v1/admin/directory/users/import/template', {
    responseType: 'blob',
  })
  return response.data as Blob
}

export async function importAdminUsers(file: File): Promise<AdminUserImportResult> {
  requireScriptApi()
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ApiEnvelope<AdminUserImportResult>>('/api/v1/admin/directory/users/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return unwrapApiData(data, '导入账号失败')
}

export async function exportAdminUsers(params?: {
  keyword?: string
  role?: string
  enabled?: boolean
  locked?: boolean
  orgUnitId?: string
  classroomId?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}): Promise<Blob> {
  requireScriptApi()
  const response = await http.get('/api/v1/admin/directory/users/export', {
    params,
    responseType: 'blob',
  })
  return response.data as Blob
}

export async function getAdminUserImportTasks(params?: { page?: number; pageSize?: number }): Promise<PagedResult<AdminUserImportTask>> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<PagedResult<AdminUserImportTask>>>('/api/v1/admin/directory/users/import/tasks', {
    params: {
      page: params?.page ?? 1,
      pageSize: params?.pageSize ?? 20,
    },
  })
  return unwrapApiData(data, '获取导入任务失败')
}

export async function getAdminUserImportTask(taskId: string): Promise<AdminUserImportTask> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<AdminUserImportTask>>(`/api/v1/admin/directory/users/import/tasks/${encodeURIComponent(taskId)}`)
  return unwrapApiData(data, '获取导入任务详情失败')
}

export async function getAdminUserBatchStats(limit = 20): Promise<AdminUserBatchStatsResponse> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<AdminUserBatchStatsResponse>>('/api/v1/admin/directory/users/batch/stats', {
    params: {
      limit,
    },
  })
  return unwrapApiData(data, '获取批量任务统计失败')
}

export async function getMediaResources(): Promise<MediaResource[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<MediaResource[]>>('/api/v1/admin/media-resources')
  return unwrapApiData(data, '获取媒体资源目录失败')
}

export async function deleteTask(taskId: string): Promise<void> {
  if (!USE_MOCK) {
    await http.delete(`/api/v1/tasks/${taskId}`)
    return
  }
  const filtered = getMockTasks().filter((t) => t.taskId !== taskId)
  saveMockTasks(filtered)
}

export function getApiBaseUrl() {
  return API_BASE_URL || 'mock-mode(localStorage)'
}

export async function getStyleTemplates(options?: { courseId?: string }): Promise<StyleTemplate[]> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<StyleTemplate[]>>('/api/v1/style-templates', courseHeaderConfig(options?.courseId))
    return unwrapApiData(data, '获取风格模板失败')
  }
  const { userId } = getClientProfile()
  const courseId = options?.courseId?.trim()
  return getMockStyleTemplates().filter((item) => {
    if (!item.enabled) return false
    if (item.scope === 'SYSTEM') return true
    if (item.scope === 'PERSONAL') return item.ownerId === userId
    return !!courseId && item.courseId === courseId
  })
}

export async function createStyleTemplate(payload: StyleTemplateCreateRequest): Promise<StyleTemplate> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<StyleTemplate>>(
      '/api/v1/style-templates',
      payload,
      courseHeaderConfig(payload.courseId),
    )
    return unwrapApiData(data, '创建风格模板失败')
  }
  const now = new Date().toISOString()
  const template: StyleTemplate = {
    templateId: `style_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`,
    scope: payload.scope ?? 'PERSONAL',
    name: payload.name.trim(),
    category: payload.category?.trim() || null,
    traits: payload.traits?.trim() || null,
    fullPrompt: payload.fullPrompt.trim(),
    styleKey: payload.styleKey?.trim() || null,
    ownerId: getOrCreateClientUserId(),
    ownerName: getOrCreateClientUserId(),
    orgUnitId: null,
    courseId: payload.courseId?.trim() || null,
    enabled: true,
    createdAt: now,
    updatedAt: now,
  }
  const all = [...getMockStyleTemplates().filter((item) => item.scope !== 'SYSTEM'), template]
  saveMockStyleTemplates(all)
  return template
}

export async function updateStyleTemplate(
  templateId: string,
  payload: StyleTemplateUpdateRequest,
): Promise<StyleTemplate> {
  if (!USE_MOCK) {
    const { data } = await http.put<ApiEnvelope<StyleTemplate>>(`/api/v1/style-templates/${encodeURIComponent(templateId)}`, payload)
    return unwrapApiData(data, '更新风格模板失败')
  }
  const all = getMockStyleTemplates()
  const idx = all.findIndex((item) => item.templateId === templateId)
  if (idx < 0) throw new Error('风格模板不存在')
  const prev = all[idx]
  const next: StyleTemplate = {
    ...prev,
    name: payload.name?.trim() || prev.name,
    category: payload.category !== undefined ? payload.category.trim() || null : prev.category,
    traits: payload.traits !== undefined ? payload.traits.trim() || null : prev.traits,
    fullPrompt: payload.fullPrompt?.trim() || prev.fullPrompt,
    styleKey: payload.styleKey !== undefined ? payload.styleKey.trim() || null : prev.styleKey,
    enabled: payload.enabled ?? prev.enabled,
    updatedAt: new Date().toISOString(),
  }
  const merged = [...all]
  merged[idx] = next
  saveMockStyleTemplates(merged)
  return next
}

export async function getAuditLogs(params?: {
  entityType?: string
  entityId?: string
  actorUserId?: string
}): Promise<AuditLogRecord[]> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<AuditLogRecord[]>>('/api/v1/audit-logs', { params })
    return unwrapApiData(data, '获取审计日志失败')
  }
  return []
}

export async function getOperationsDashboard(): Promise<OperationsDashboardResponse> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<OperationsDashboardResponse>>('/api/v1/operations/dashboard')
  return unwrapApiData(data, '获取统计看板失败')
}

export async function getCourses(): Promise<TeachingCourse[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<TeachingCourse[]>>('/api/v1/courses')
  return unwrapApiData(data, '获取课程列表失败')
}

export async function createCourse(payload: CourseCreateRequest): Promise<TeachingCourse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<TeachingCourse>>('/api/v1/courses', payload)
  return unwrapApiData(data, '创建课程失败')
}

export async function archiveCourse(courseId: string, payload?: TeachingCourseArchiveRequest): Promise<TeachingCourse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<TeachingCourse>>(
    `/api/v1/courses/${encodeURIComponent(courseId)}/archive`,
    payload ?? { archived: true },
    courseHeaderConfig(courseId),
  )
  return unwrapApiData(data, '更新课程归档状态失败')
}

export async function getCourseAssignments(courseId: string): Promise<TeachingAssignment[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<TeachingAssignment[]>>(
    `/api/v1/courses/${encodeURIComponent(courseId)}/assignments`,
    courseHeaderConfig(courseId),
  )
  return unwrapApiData(data, '获取作业列表失败')
}

export async function createCourseAssignment(
  courseId: string,
  payload: AssignmentCreateRequest,
): Promise<TeachingAssignment> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<TeachingAssignment>>(
    `/api/v1/courses/${encodeURIComponent(courseId)}/assignments`,
    payload,
    courseHeaderConfig(courseId),
  )
  return unwrapApiData(data, '创建作业失败')
}

export async function updateCourseAssignmentStatus(
  courseId: string,
  assignmentId: string,
  payload: TeachingAssignmentStatusUpdateRequest,
): Promise<TeachingAssignment> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<TeachingAssignment>>(
    `/api/v1/courses/${encodeURIComponent(courseId)}/assignments/${encodeURIComponent(assignmentId)}/status`,
    payload,
    courseHeaderConfig(courseId),
  )
  return unwrapApiData(data, '更新作业状态失败')
}

export async function getAssignmentSubmissions(assignmentId: string): Promise<AssignmentSubmission[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<AssignmentSubmission[]>>(
    `/api/v1/assignments/${encodeURIComponent(assignmentId)}/submissions`,
  )
  return unwrapApiData(data, '获取提交列表失败')
}

export async function createAssignmentSubmission(
  assignmentId: string,
  payload: SubmissionCreateRequest,
): Promise<AssignmentSubmission> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AssignmentSubmission>>(
    `/api/v1/assignments/${encodeURIComponent(assignmentId)}/submissions`,
    payload,
  )
  return unwrapApiData(data, '提交作业失败')
}

export async function reviewAssignmentSubmission(
  submissionId: string,
  payload: SubmissionReviewRequest,
): Promise<AssignmentSubmission> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AssignmentSubmission>>(
    `/api/v1/submissions/${encodeURIComponent(submissionId)}/review`,
    payload,
  )
  return unwrapApiData(data, '评分失败')
}

export async function getSubmissionReviews(submissionId: string): Promise<ReviewRecord[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ReviewRecord[]>>(
    `/api/v1/submissions/${encodeURIComponent(submissionId)}/reviews`,
  )
  return unwrapApiData(data, '获取评审记录失败')
}

export async function getConnections(): Promise<ConnectionConfig[]> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<ConnectionConfig[]>>('/api/v1/connections')
    return unwrapApiData(data, '获取连接列表失败')
  }
  return []
}

const MOCK_PRESET_MODELS: PresetModelListResponse = {
  providers: ['openai', 'anthropic', 'deepseek', 'qwen', 'ark', 'onelinkai', 'vidu', 'moark'],
  models: [
    { provider: 'openai', modelName: 'gpt-4o', baseUrl: 'https://api.openai.com', displayName: 'GPT-4o', capabilities: ['text'] },
    { provider: 'openai', modelName: 'gpt-4o-mini', baseUrl: 'https://api.openai.com', displayName: 'GPT-4o Mini', capabilities: ['text'] },
    { provider: 'anthropic', modelName: 'claude-sonnet-4-6', baseUrl: 'https://api.anthropic.com', displayName: 'Claude Sonnet 4.6', capabilities: ['text'] },
    { provider: 'deepseek', modelName: 'deepseek-chat', baseUrl: 'https://api.deepseek.com', displayName: 'DeepSeek Chat', capabilities: ['text'] },
    { provider: 'qwen', modelName: 'qwen-plus', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode', displayName: '通义千问 Plus', capabilities: ['text'] },
    { provider: 'ark', modelName: 'doubao-seedream-5-0-260128', baseUrl: 'https://ark.cn-beijing.volces.com', displayName: '豆包图片 Seedream', capabilities: ['image'] },
    { provider: 'onelinkai', modelName: 'gpt-4o', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI GPT-4o', capabilities: ['text'] },
    { provider: 'onelinkai', modelName: 'claude-sonnet-4-6', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Claude Sonnet 4.6', capabilities: ['text'] },
    { provider: 'onelinkai', modelName: 'gemini-2.5-pro', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Gemini 2.5 Pro', capabilities: ['text'] },
    { provider: 'onelinkai', modelName: 'wanx-v1', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Wanx v1', capabilities: ['image'] },
    { provider: 'onelinkai', modelName: 'kling-v2-1', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Kling 图像生成 v2.1', capabilities: ['image'] },
    { provider: 'onelinkai', modelName: 'kling-v2', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Kling 多图参考生图 v2', capabilities: ['image'] },
    { provider: 'onelinkai', modelName: 'MiniMax-M2.1', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI MiniMax M2.1', capabilities: ['video'] },
    { provider: 'onelinkai', modelName: 'kling-v2-6', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Kling 文生视频 v2.6', capabilities: ['video'] },
    { provider: 'onelinkai', modelName: 'kling-v1', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Kling 图生视频 v1', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq3-turbo', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q3 Turbo', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq3-pro', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q3 Pro', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq2-pro-fast', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q2 Pro Fast', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq2-pro', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q2 Pro', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq2-turbo', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q2 Turbo', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq2', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q2', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq1', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q1', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'viduq1-classic', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu Q1 Classic', capabilities: ['video'] },
    { provider: 'vidu', modelName: 'vidu2.0', baseUrl: 'https://api.vidu.cn', displayName: 'Vidu 2.0', capabilities: ['video'] },
    {
      provider: 'moark',
      modelName: 'Wan2.1-I2V-14B-720P',
      baseUrl: 'https://api.moark.com',
      displayName: 'Moark Wan2.1 图生视频 720P',
      capabilities: ['video'],
    },
  ],
}

export async function getPresetModels(): Promise<PresetModelListResponse> {
  if (USE_MOCK) {
    return MOCK_PRESET_MODELS
  }
  const { data } = await http.get<ApiEnvelope<PresetModelListResponse>>('/api/v1/preset-models')
  return unwrapApiData(data, '获取预置模型列表失败')
}

/** Matches server ProviderCatalog for mock/offline UI. */
const MOCK_PROVIDER_CATALOG: ProviderCatalogListResponse = {
  providers: [
    {
      key: 'openai',
      displayName: 'OpenAI',
      defaultBaseUrl: 'https://api.openai.com',
      authMode: 'BEARER',
      apiFormat: 'openai',
      gatewayKind: 'OPENAI_COMPAT',
      textProxySupported: true,
      imageProxySupported: true,
      videoProxySupported: false,
      staticModels: [],
    },
    {
      key: 'anthropic',
      displayName: 'Anthropic',
      defaultBaseUrl: 'https://api.anthropic.com',
      authMode: 'X_API_KEY',
      apiFormat: 'anthropic',
      gatewayKind: 'ANTHROPIC',
      textProxySupported: true,
      imageProxySupported: false,
      videoProxySupported: false,
      staticModels: [],
    },
    {
      key: 'deepseek',
      displayName: 'DeepSeek',
      defaultBaseUrl: 'https://api.deepseek.com',
      authMode: 'BEARER',
      apiFormat: 'openai',
      gatewayKind: 'OPENAI_COMPAT',
      textProxySupported: true,
      imageProxySupported: false,
      videoProxySupported: false,
      staticModels: [],
    },
    {
      key: 'qwen',
      displayName: '通义千问',
      defaultBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
      authMode: 'BEARER',
      apiFormat: 'openai',
      gatewayKind: 'OPENAI_COMPAT',
      textProxySupported: true,
      imageProxySupported: false,
      videoProxySupported: false,
      staticModels: [],
    },
    {
      key: 'ollama',
      displayName: 'Ollama',
      defaultBaseUrl: 'http://localhost:11434',
      authMode: 'NONE',
      apiFormat: 'openai',
      gatewayKind: 'OLLAMA',
      textProxySupported: true,
      imageProxySupported: false,
      videoProxySupported: false,
      staticModels: [],
    },
    {
      key: 'onelinkai',
      displayName: 'OneLinkAI',
      defaultBaseUrl: 'https://api.onelinkai.cloud',
      authMode: 'BEARER',
      apiFormat: 'openai',
      gatewayKind: 'OPENAI_COMPAT',
      textProxySupported: true,
      imageProxySupported: true,
      videoProxySupported: true,
      staticModels: [
        'gpt-4o',
        'claude-sonnet-4-6',
        'gemini-2.5-pro',
        'wanx-v1',
        'kling-v1',
        'kling-v1-6',
        'kling-v2',
        'kling-v2-1',
        'kling-v2-6',
        'MiniMax-M2.1',
        'viduq3-turbo',
        'viduq3-pro',
        'viduq2-pro-fast',
        'viduq2-pro',
        'viduq2-turbo',
        'viduq2',
        'viduq1',
        'viduq1-classic',
        'vidu2.0',
      ],
    },
    {
      key: 'ark',
      displayName: '方舟',
      defaultBaseUrl: 'https://ark.cn-beijing.volces.com',
      authMode: 'BEARER',
      apiFormat: 'openai',
      gatewayKind: 'OPENAI_COMPAT',
      textProxySupported: true,
      imageProxySupported: true,
      videoProxySupported: true,
      staticModels: [],
    },
    {
      key: 'vidu',
      displayName: 'Vidu',
      defaultBaseUrl: 'https://api.vidu.cn',
      authMode: 'TOKEN',
      apiFormat: 'vidu',
      gatewayKind: 'OPENAI_COMPAT',
      textProxySupported: false,
      imageProxySupported: false,
      videoProxySupported: true,
      staticModels: [
        'viduq3-turbo',
        'viduq3-pro',
        'viduq2-pro-fast',
        'viduq2-pro',
        'viduq2-turbo',
        'viduq2',
        'viduq1',
        'viduq1-classic',
        'vidu2.0',
      ],
    },
  ],
}

export async function getProviderCatalog(): Promise<ProviderCatalogListResponse> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<ProviderCatalogListResponse>>('/api/v1/provider-catalog')
    return unwrapApiData(data, '获取服务商目录失败')
  }
  return MOCK_PROVIDER_CATALOG
}

export async function getProviderOAuthNotes(): Promise<Record<string, string>> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<Record<string, string>>>('/api/v1/provider-catalog/oauth-notes')
    return unwrapApiData(data, '获取桌面能力说明失败')
  }
  return {
    message:
      'GitHub Copilot、CherryIN 等依赖 Electron/OAuth 桌面流程的提供商未在本 Web 网关实现；请使用 Cherry Studio 客户端或接入兼容 OpenAI 的代理。',
    ovms: 'OVMS 模型下载需本地运行时；本 Web 应用不提供与 Cherry 桌面相同的下载向导。',
    copilot: 'GitHub Copilot 需 OAuth；Web 侧请改用 OpenAI 兼容中转或官方 API Key。',
  }
}

export async function quickCreateConnection(req: QuickConnectionRequest): Promise<ConnectionConfig> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<ConnectionConfig>>('/api/v1/connections/quick', req)
    return unwrapApiData(data, '快捷创建失败')
  }
  throw new Error('Mock mode not supported')
}

export async function createConnection(req: ConnectionConfigCreateRequest): Promise<ConnectionConfig> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<ConnectionConfig>>('/api/v1/connections', req)
    return unwrapApiData(data, '创建连接失败')
  }
  throw new Error('Mock mode not supported')
}

export async function updateConnection(id: string, req: ConnectionConfigUpdateRequest): Promise<ConnectionConfig> {
  if (!USE_MOCK) {
    const { data } = await http.put<ApiEnvelope<ConnectionConfig>>(`/api/v1/connections/${id}`, req)
    return unwrapApiData(data, '更新连接失败')
  }
  throw new Error('Mock mode not supported')
}

export async function deleteConnection(id: string): Promise<void> {
  if (!USE_MOCK) {
    await http.delete(`/api/v1/connections/${id}`)
    return
  }
  throw new Error('Mock mode not supported')
}

export async function testConnection(id: string): Promise<ConnectionTestResponse> {
  const { data } = await http.post<ApiEnvelope<ConnectionTestResponse>>(`/api/v1/connections/${id}/test`)
  return unwrapApiData(data, '连接测试失败')
}

export async function getModels(): Promise<ModelConfig[]> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<ModelConfig[]>>('/api/v1/models')
    return unwrapApiData(data, '获取模型列表失败')
  }
  return []
}

export async function createModel(req: ModelConfigCreateRequest): Promise<ModelConfig> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<ModelConfig>>('/api/v1/models', req)
    return unwrapApiData(data, '创建模型失败')
  }
  throw new Error('Mock mode not supported')
}

export async function batchImportModels(req: BatchModelsImportRequest): Promise<ModelConfig[]> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<ModelConfig[]>>('/api/v1/models/batch-import', req)
    return unwrapApiData(data, '批量导入失败')
  }
  return []
}

export async function probeModel(id: string): Promise<ModelProbeResponse> {
  if (!USE_MOCK) {
    const { data } = await http.post<ApiEnvelope<ModelProbeResponse>>(`/api/v1/models/${id}/probe`)
    return unwrapApiData(data, '探测失败')
  }
  return { ok: true, message: 'mock' }
}

export async function updateModel(id: string, req: ModelConfigUpdateRequest): Promise<ModelConfig> {
  if (!USE_MOCK) {
    const { data } = await http.put<ApiEnvelope<ModelConfig>>(`/api/v1/models/${id}`, req)
    return unwrapApiData(data, '更新模型失败')
  }
  throw new Error('Mock mode not supported')
}

export async function deleteModel(id: string): Promise<void> {
  if (!USE_MOCK) {
    await http.delete(`/api/v1/models/${id}`)
    return
  }
  throw new Error('Mock mode not supported')
}

function requireScriptApi() {
  if (USE_MOCK) {
    throw new Error('剧本工作流需要先启动后端服务并配置 `VITE_API_BASE_URL`')
  }
}

export async function listScriptProjects(options?: { deleted?: boolean }): Promise<ScriptProjectSummary[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ScriptProjectSummary[]>>('/api/v1/script-projects', {
    params: {
      deleted: options?.deleted ? 'true' : undefined,
    },
  })
  return unwrapApiData(data, '获取剧本工程列表失败')
}

export async function createScriptProject(payload: ScriptProjectCreateRequest): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>(
    '/api/v1/script-projects',
    payload,
    courseHeaderConfig(payload.courseId),
  )
  return unwrapApiData(data, '创建剧本工程失败')
}

export async function uploadScriptProject(payload: ScriptProjectUploadRequest): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const formData = new FormData()
  formData.append('name', payload.name)
  formData.append('file', payload.file)
  if (payload.visualStyle) formData.append('visualStyle', payload.visualStyle)
  if (payload.styleTemplateId) formData.append('styleTemplateId', payload.styleTemplateId)
  if (payload.aspectRatio) formData.append('aspectRatio', payload.aspectRatio)
  if (payload.targetDuration != null) formData.append('targetDuration', String(payload.targetDuration))
  if (payload.language) formData.append('language', payload.language)
  if (payload.courseId) formData.append('courseId', payload.courseId)
  if (payload.explicitTextModel) formData.append('explicitTextModel', payload.explicitTextModel)
  if (payload.explicitImageModel) formData.append('explicitImageModel', payload.explicitImageModel)
  if (payload.explicitVideoModel) formData.append('explicitVideoModel', payload.explicitVideoModel)
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>(
    '/api/v1/script-projects/upload',
    formData,
    courseHeaderConfig(payload.courseId),
  )
  return unwrapApiData(data, '上传剧本失败')
}

export async function getScriptProject(projectId: string): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ScriptProjectAggregate>>(`/api/v1/script-projects/${projectId}`)
  return normalizeScriptProjectAggregate(unwrapApiData(data, '获取剧本工程详情失败'))
}

export async function deleteScriptProject(projectId: string): Promise<void> {
  requireScriptApi()
  const path = `/api/v1/script-projects/${encodeURIComponent(projectId)}`
  try {
    const { data } = await http.delete<ApiEnvelope<null>>(path)
    unwrapApiVoid(data, '删除剧本工程失败')
  } catch (e) {
    throw enhanceAxiosError(e)
  }
}

export async function restoreScriptProject(projectId: string): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/restore`,
  )
  return unwrapApiData(data, '恢复剧本工程失败')
}

export async function refineScriptProject(projectId: string): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(`/api/v1/script-projects/${projectId}/refine`)
  return normalizeScriptDocumentPayload(unwrapApiData(data, '完善剧本失败'))
}

export async function refineScriptProjectWithPrompt(
  projectId: string,
  briefPrompt: string,
): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/refine-with-brief`,
    { briefPrompt },
  )
  return normalizeScriptDocumentPayload(unwrapApiData(data, '完善剧本失败'))
}

export async function getScriptProjectDocument(projectId: string): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ScriptDocumentPayload>>(`/api/v1/script-projects/${projectId}/script`)
  return normalizeScriptDocumentPayload(unwrapApiData(data, '获取剧本内容失败'))
}

export async function updateScriptProjectDocument(projectId: string, payload: UpdateScriptRequest): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<ScriptDocumentPayload>>(`/api/v1/script-projects/${projectId}/script`, payload)
  return normalizeScriptDocumentPayload(unwrapApiData(data, '保存剧本失败'))
}

export async function importScriptToProject(
  projectId: string,
  file: File,
  options?: { replaceName?: string; autoRefine?: boolean },
): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const formData = new FormData()
  formData.append('file', file)
  if (options?.replaceName) formData.append('replaceName', options.replaceName)
  if (options?.autoRefine) formData.append('autoRefine', 'true')
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/import`,
    formData,
  )
  return unwrapApiData(data, '导入剧本失败')
}

export async function listScriptRevisions(projectId: string): Promise<ScriptRevision[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ScriptRevision[]>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/revisions`,
  )
  return unwrapApiData(data, '获取修订列表失败')
}

export async function restoreScriptRevision(projectId: string, revisionId: string): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/revisions/${encodeURIComponent(revisionId)}/restore`,
  )
  return normalizeScriptDocumentPayload(unwrapApiData(data, '恢复修订失败'))
}

export async function optimizeScriptScenes(projectId: string): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/optimize/scenes`,
  )
  return normalizeScriptDocumentPayload(unwrapApiData(data, '场景优化失败'))
}

export async function optimizeScriptCharacters(projectId: string): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/optimize/characters`,
  )
  return normalizeScriptDocumentPayload(unwrapApiData(data, '角色优化失败'))
}

export async function optimizeScriptProps(projectId: string): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/optimize/props`,
  )
  return normalizeScriptDocumentPayload(unwrapApiData(data, '道具优化失败'))
}

export async function appendScriptProjectPreview(
  projectId: string,
  payload?: AppendScriptPreviewRequest,
): Promise<AppendScriptPreviewResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<AppendScriptPreviewResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/append/preview`,
    payload || {},
  )
  return unwrapApiData(data, '续写预览失败')
}

export async function rewriteScriptProjectPreview(
  projectId: string,
  payload: RewriteScriptPreviewRequest,
): Promise<RewriteScriptPreviewResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<RewriteScriptPreviewResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/rewrite/preview`,
    payload,
  )
  return unwrapApiData(data, '改写预览失败')
}

export async function applyRewriteScriptProject(
  projectId: string,
  payload: RewriteScriptApplyRequest,
): Promise<ScriptDocumentPayload> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptDocumentPayload>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/rewrite/apply`,
    payload,
  )
  return normalizeScriptDocumentPayload(unwrapApiData(data, '应用改写失败'))
}

export async function extractScriptAssets(projectId: string, type: 'characters' | 'backgrounds' | 'props'): Promise<ExtractedAsset[]> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ExtractedAsset[]>>(`/api/v1/script-projects/${projectId}/assets/extract/${type}`)
  return unwrapApiData(data, '抽取资产失败')
}

export async function getScriptAssets(projectId: string): Promise<ExtractedAsset[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ExtractedAsset[]>>(`/api/v1/script-projects/${projectId}/assets`)
  return unwrapApiData(data, '获取资产列表失败')
}

export async function updateScriptAsset(projectId: string, assetId: string, payload: UpdateAssetRequest): Promise<ExtractedAsset> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<ExtractedAsset>>(`/api/v1/script-projects/${projectId}/assets/${assetId}`, payload)
  return unwrapApiData(data, '保存资产失败')
}

export async function generateAssetKeyframes(projectId: string, assetId: string): Promise<KeyframeRecord[]> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<KeyframeRecord[]>>(`/api/v1/script-projects/${projectId}/assets/${assetId}/keyframes/generate`)
  return unwrapApiData(data, '生成关键帧失败')
}

export async function getProjectKeyframes(projectId: string): Promise<KeyframeRecord[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<KeyframeRecord[]>>(`/api/v1/script-projects/${projectId}/keyframes`)
  return unwrapApiData(data, '获取关键帧列表失败')
}

export async function confirmProjectKeyframe(projectId: string, keyframeId: string): Promise<KeyframeRecord> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<KeyframeRecord>>(`/api/v1/script-projects/${projectId}/keyframes/${keyframeId}/confirm`)
  return unwrapApiData(data, '确认关键帧失败')
}

export async function regenerateProjectKeyframe(projectId: string, keyframeId: string): Promise<KeyframeRecord[]> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<KeyframeRecord[]>>(`/api/v1/script-projects/${projectId}/keyframes/${keyframeId}/regenerate`)
  return unwrapApiData(data, '重生成关键帧失败')
}

export async function generateArtDirection(projectId: string): Promise<ArtDirectionResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ArtDirectionResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/art-direction/generate`,
  )
  return unwrapApiData(data, '生成美术指导失败')
}

export async function getArtDirection(projectId: string): Promise<ArtDirectionResponse> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ArtDirectionResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/art-direction`,
  )
  return unwrapApiData(data, '获取美术指导失败')
}

export async function batchGenerateCharacterVisualPrompts(projectId: string): Promise<BatchVisualPromptResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<BatchVisualPromptResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/visual-prompt/batch-generate`,
  )
  return unwrapApiData(data, '批量生成角色视觉提示词失败')
}

export async function generateAssetVisualPrompt(projectId: string, assetId: string): Promise<VisualPromptResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<VisualPromptResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/visual-prompt/generate`,
  )
  return unwrapApiData(data, '生成视觉提示词失败')
}

export async function generateTurnaroundPlan(projectId: string, assetId: string): Promise<TurnaroundPlanResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<TurnaroundPlanResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/turnaround/plan`,
  )
  return unwrapApiData(data, '生成九宫格规划失败')
}

export async function generateTurnaroundImage(projectId: string, assetId: string): Promise<TurnaroundImageResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<TurnaroundImageResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/turnaround/generate`,
  )
  return unwrapApiData(data, '生成九宫格造型图失败')
}

export async function generateStoryboardPlan(projectId: string, assetId: string): Promise<StoryboardPlanResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardPlanResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/storyboard/plan`,
  )
  return unwrapApiData(data, '生成九宫格分镜规划失败')
}

export async function translateStoryboardPlan(projectId: string, assetId: string): Promise<StoryboardPlanResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardPlanResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/storyboard/translate`,
  )
  return unwrapApiData(data, '翻译九宫格分镜失败')
}

export async function rewriteStoryboardPlan(
  projectId: string,
  assetId: string,
  payload: StoryboardRewriteRequest,
): Promise<StoryboardPlanResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardPlanResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/storyboard/rewrite`,
    payload,
  )
  return unwrapApiData(data, '改写九宫格分镜失败')
}

export async function generateStoryboardImage(projectId: string, assetId: string): Promise<StoryboardImageResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardImageResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/storyboard/image`,
  )
  return unwrapApiData(data, '生成九宫格分镜图失败')
}

export async function cropStoryboardPanel(
  projectId: string,
  assetId: string,
  panelIndex: number,
): Promise<StoryboardPanelCropResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardPanelCropResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/storyboard/panels/${encodeURIComponent(String(panelIndex))}/crop`,
  )
  return unwrapApiData(data, '裁剪九宫格分镜失败')
}

export async function generateThreeView(projectId: string, assetId: string): Promise<ThreeViewResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ThreeViewResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/three-view/generate`,
  )
  return unwrapApiData(data, '生成三视图失败')
}

export async function generateGroupScene(projectId: string, payload: GenerateGroupSceneRequest): Promise<GroupSceneResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<GroupSceneResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/visual/group-scene`,
    payload,
  )
  return unwrapApiData(data, '生成群像提示词失败')
}

export async function generateShotVisualPrompt(projectId: string, shotId: string): Promise<ShotVisualPromptResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ShotVisualPromptResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/shots/${encodeURIComponent(shotId)}/visual-prompt/generate`,
  )
  return unwrapApiData(data, '生成分镜提示词失败')
}

export async function applyStoryboardFirstFrame(
  projectId: string,
  shotId: string,
  payload: ApplyStoryboardFirstFrameRequest,
): Promise<StoryboardFirstFrameResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardFirstFrameResponse>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/shots/${encodeURIComponent(shotId)}/storyboard-first-frame/apply`,
    payload,
  )
  return unwrapApiData(data, '应用九宫格首帧失败')
}

export async function splitScriptProjectShots(projectId: string): Promise<StoryboardShot[]> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardShot[]>>(`/api/v1/script-projects/${projectId}/shots/split`)
  return unwrapApiData(data, '拆分镜头失败')
}

export async function getScriptProjectShots(projectId: string): Promise<StoryboardShot[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<StoryboardShot[]>>(`/api/v1/script-projects/${projectId}/shots`)
  return unwrapApiData(data, '获取镜头列表失败')
}

export async function updateScriptProjectShot(
  projectId: string,
  shotId: string,
  payload: UpdateShotRequest,
): Promise<StoryboardShot> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<StoryboardShot>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/shots/${encodeURIComponent(shotId)}`,
    payload,
  )
  return unwrapApiData(data, '更新镜头参数失败')
}

export async function rollbackShotVisualPrompt(
  projectId: string,
  shotId: string,
  payload: { versionId: string },
): Promise<StoryboardShot> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<StoryboardShot>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/shots/${encodeURIComponent(shotId)}/visual-prompt/rollback`,
    payload,
  )
  return unwrapApiData(data, '回滚镜头提示词失败')
}

export async function rollbackAssetVisualPrompt(
  projectId: string,
  assetId: string,
  payload: { versionId: string },
): Promise<ExtractedAsset> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ExtractedAsset>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/assets/${encodeURIComponent(assetId)}/visual-prompt/rollback`,
    payload,
  )
  return unwrapApiData(data, '回滚资产提示词失败')
}

export async function updateKeyframePromptText(
  projectId: string,
  keyframeId: string,
  payload: { promptText: string },
): Promise<KeyframeRecord> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<KeyframeRecord>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/keyframes/${encodeURIComponent(keyframeId)}/prompt`,
    payload,
  )
  return unwrapApiData(data, '更新关键帧提示词失败')
}

export async function rollbackKeyframePrompt(
  projectId: string,
  keyframeId: string,
  payload: { versionId: string },
): Promise<KeyframeRecord> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<KeyframeRecord>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/keyframes/${encodeURIComponent(keyframeId)}/prompt/rollback`,
    payload,
  )
  return unwrapApiData(data, '回滚关键帧提示词失败')
}

export async function getPromptTemplateCatalog(): Promise<PromptTemplateCatalogItem[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<PromptTemplateCatalogItem[]>>('/api/v1/prompt-templates/catalog')
  return unwrapApiData(data, '获取提示词模板目录失败')
}

export async function getPromptTemplateOverrides(projectId: string): Promise<Record<string, string>> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<Record<string, string>>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/prompt-template-overrides`,
  )
  return unwrapApiData(data, '获取模板覆盖失败')
}

export async function updatePromptTemplateOverrides(
  projectId: string,
  overrides: Record<string, string>,
): Promise<Record<string, string>> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<Record<string, string>>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/prompt-template-overrides`,
    { overrides },
  )
  return unwrapApiData(data, '保存模板覆盖失败')
}

export async function generateScriptProjectVideos(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/video/generate`)
  return unwrapApiData(data, '启动视频生成失败')
}

export async function getScriptProjectVideoTasks(projectId: string): Promise<VideoSegmentTask[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<VideoSegmentTask[]>>(`/api/v1/script-projects/${projectId}/video/tasks`)
  return unwrapApiData(data, '获取视频任务失败')
}

export async function retryScriptProjectVideoTask(projectId: string, segmentTaskId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/video/tasks/${segmentTaskId}/retry`)
  return unwrapApiData(data, '重试视频任务失败')
}

export async function generateScriptProjectDubbing(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/dubbing/generate`)
  return unwrapApiData(data, '启动配音生成失败')
}

export async function getScriptProjectDubbingTasks(projectId: string): Promise<DubbingTask[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<DubbingTask[]>>(`/api/v1/script-projects/${projectId}/dubbing/tasks`)
  return unwrapApiData(data, '获取配音任务失败')
}

export async function retryScriptProjectDubbingTask(projectId: string, dubbingTaskId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/dubbing/tasks/${dubbingTaskId}/retry`)
  return unwrapApiData(data, '重试配音任务失败')
}

export async function generateScriptProjectLipSync(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/lip-sync/generate`)
  return unwrapApiData(data, '启动口型同步失败')
}

export async function getScriptProjectLipSyncTasks(projectId: string): Promise<LipSyncTask[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<LipSyncTask[]>>(`/api/v1/script-projects/${projectId}/lip-sync/tasks`)
  return unwrapApiData(data, '获取口型同步任务失败')
}

export async function retryScriptProjectLipSyncTask(projectId: string, lipSyncTaskId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/lip-sync/tasks/${lipSyncTaskId}/retry`)
  return unwrapApiData(data, '重试口型同步任务失败')
}

export async function getScriptProjectVideoEditingDraft(projectId: string): Promise<VideoEditingDraft> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<unknown>>(`/api/v1/script-projects/${projectId}/video-editing/draft`)
  return normalizeVideoEditingDraftResponse(unwrapApiData(data, '获取视频剪辑草稿失败'))
}

export async function getScriptProjectVideoEditingRenderTasks(projectId: string): Promise<VideoEditingRenderTask[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<unknown[]>>(`/api/v1/script-projects/${projectId}/video-editing/render/tasks`)
  return unwrapApiData(data, '获取剪辑渲染任务失败').map(normalizeVideoEditingRenderTask)
}

export async function saveScriptProjectVideoEditingDraft(
  projectId: string,
  payload: VideoEditingSaveDraftRequest,
): Promise<VideoEditingDraft> {
  requireScriptApi()
  const { data } = await http.put<ApiEnvelope<unknown>>(
    `/api/v1/script-projects/${projectId}/video-editing/draft`,
    toVideoEditingSaveDraftPayload(payload),
  )
  return normalizeVideoEditingDraftResponse(unwrapApiData(data, '保存视频剪辑草稿失败'))
}

export async function resetScriptProjectVideoEditingDraft(projectId: string): Promise<VideoEditingDraft> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<unknown>>(`/api/v1/script-projects/${projectId}/video-editing/draft/reset`)
  return normalizeVideoEditingDraftResponse(unwrapApiData(data, '重置视频剪辑草稿失败'))
}

export async function renderScriptProjectVideoEditingPreview(
  projectId: string,
  payload?: VideoEditingRenderRequest,
): Promise<PipelineStatus> {
  requireScriptApi()
  void payload
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(
    `/api/v1/script-projects/${projectId}/video-editing/render/preview`,
  )
  return normalizePipelineStatus(unwrapApiData(data, '发起剪辑预览渲染失败'))
}

export async function publishScriptProjectVideoEditingResult(
  projectId: string,
  payload?: VideoEditingPublishRequest,
): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(
    `/api/v1/script-projects/${projectId}/video-editing/render/publish`,
    toVideoEditingPublishPayload(payload),
  )
  return normalizePipelineStatus(unwrapApiData(data, '发布剪辑成片失败'))
}

export async function retryScriptProjectVideoEditingRenderTask(
  projectId: string,
  renderTaskId: string,
): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(
    `/api/v1/script-projects/${projectId}/video-editing/render/tasks/${encodeURIComponent(renderTaskId)}/retry`,
  )
  return normalizePipelineStatus(unwrapApiData(data, '重试剪辑渲染任务失败'))
}

export async function generateScriptProjectFinalComposition(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/final-composition/generate`)
  return unwrapApiData(data, '启动成片编排失败')
}

export async function getScriptProjectFinalCompositionTasks(projectId: string): Promise<FinalCompositionTask[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<FinalCompositionTask[]>>(`/api/v1/script-projects/${projectId}/final-composition/tasks`)
  return unwrapApiData(data, '获取成片任务失败')
}

export async function retryScriptProjectFinalCompositionTask(projectId: string, finalCompositionTaskId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(
    `/api/v1/script-projects/${projectId}/final-composition/tasks/${finalCompositionTaskId}/retry`,
  )
  return unwrapApiData(data, '重试成片任务失败')
}

export async function generateScriptProjectExportPackage(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/export-package/generate`)
  return unwrapApiData(data, '启动导出包生成失败')
}

export async function getScriptProjectExportPackageTasks(projectId: string): Promise<ExportPackageTask[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ExportPackageTask[]>>(`/api/v1/script-projects/${projectId}/export-package/tasks`)
  return unwrapApiData(data, '获取导出包任务失败')
}

export async function retryScriptProjectExportPackageTask(projectId: string, exportPackageTaskId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<PipelineStatus>>(
    `/api/v1/script-projects/${projectId}/export-package/tasks/${exportPackageTaskId}/retry`,
  )
  return unwrapApiData(data, '重试导出包任务失败')
}

export async function getScriptProjectContentReviewStatus(projectId: string): Promise<ContentReviewStatusResponse> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ContentReviewStatusResponse>>(`/api/v1/script-projects/${projectId}/content-review`)
  return unwrapApiData(data, '获取审核状态失败')
}

export async function submitScriptProjectContentReview(
  projectId: string,
  payload?: ContentReviewSubmitRequest,
): Promise<ContentReviewStatusResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ContentReviewStatusResponse>>(
    `/api/v1/script-projects/${projectId}/content-review/submit`,
    payload ?? {},
  )
  return unwrapApiData(data, '提交审核失败')
}

export async function approveScriptProjectContentReview(
  projectId: string,
  payload?: ContentReviewDecisionRequest,
): Promise<ContentReviewStatusResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ContentReviewStatusResponse>>(
    `/api/v1/script-projects/${projectId}/content-review/approve`,
    payload ?? {},
  )
  return unwrapApiData(data, '审核通过失败')
}

export async function rejectScriptProjectContentReview(
  projectId: string,
  payload?: ContentReviewDecisionRequest,
): Promise<ContentReviewStatusResponse> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ContentReviewStatusResponse>>(
    `/api/v1/script-projects/${projectId}/content-review/reject`,
    payload ?? {},
  )
  return unwrapApiData(data, '驳回审核失败')
}

export async function getScriptProjectPipelineStatus(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/pipeline-status`)
  return normalizePipelineStatus(unwrapApiData(data, '获取流水线状态失败'))
}

// ── Workflow model settings ────────────────────────────────────────────────
export async function getWorkflowModelSettings(projectId: string): Promise<WorkflowModelSettings> {
  const { data } = await http.get<ApiEnvelope<WorkflowModelSettings>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/model-settings`
  )
  return unwrapApiData(data, '获取模型设置失败')
}

export async function updateWorkflowModelSettings(
  projectId: string,
  request: WorkflowModelSettingsUpdateRequest
): Promise<WorkflowModelSettings> {
  const { data } = await http.put<ApiEnvelope<WorkflowModelSettings>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/model-settings`,
    request
  )
  return unwrapApiData(data, '保存模型设置失败')
}

export function resolveScriptFileUrl(fileId?: string | null): string {
  if (!fileId) return ''
  if (!API_BASE_URL) return `/api/v1/files/${fileId}`
  const normalized = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
  return `${normalized}/api/v1/files/${fileId}`
}

/** 工作台生成结果：支持外链 URL、已落盘的 `/api/v1/files/...` 路径、或裸 fileId */
export function resolveApiMediaUrl(pathOrUrl: string): string {
  if (!pathOrUrl) return ''
  if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) return pathOrUrl
  if (pathOrUrl.startsWith('/')) {
    if (!API_BASE_URL) return pathOrUrl
    const normalized = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
    return `${normalized}${pathOrUrl}`
  }
  return resolveScriptFileUrl(pathOrUrl)
}

export async function listAssetHistory(
  projectId: string,
  params?: { type?: AssetHistoryType; referenceId?: string },
): Promise<AssetGenerationHistoryItem[]> {
  const { data } = await http.get<ApiEnvelope<AssetGenerationHistoryItem[]>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/asset-history`,
    {
      params: {
        type: params?.type,
        referenceId: params?.referenceId,
      },
    },
  )
  return unwrapApiData(data, '获取资产历史失败')
}

export async function restoreAssetHistory(projectId: string, historyId: number): Promise<AssetGenerationHistoryItem> {
  const { data } = await http.post<ApiEnvelope<AssetGenerationHistoryItem>>(
    `/api/v1/script-projects/${encodeURIComponent(projectId)}/asset-history/${historyId}/restore`,
  )
  return unwrapApiData(data, '恢复历史版本失败')
}

// ── Teaching Assignment Stats ────────────────────────────────────────────
export interface AssignmentStats {
  assignmentId: string
  assignmentTitle: string
  totalStudents: number
  submittedCount: number
  pendingReviewCount: number
  reviewedCount: number
  returnedCount: number
  averageScore: number | null
  maxScore: number
  minScore: number
  scoreDistribution: Record<number, number>
  scoreBuckets: Array<{ label: string; minScore: number; maxScore: number; count: number }>
}

export async function getAssignmentStats(assignmentId: string): Promise<AssignmentStats> {
  const { data } = await http.get<ApiEnvelope<AssignmentStats>>(
    `/api/v1/assignments/${encodeURIComponent(assignmentId)}/stats`,
  )
  return unwrapApiData(data, '获取作业统计失败')
}

export interface BatchReviewRequest {
  submissionIds: string[]
  status: SubmissionStatus
  score: number
  comment?: string
}

export interface BatchReviewResponse {
  totalRequested: number
  successCount: number
  failedCount: number
  failedItems: Array<{ submissionId: string; reason: string }>
}

export async function batchReviewSubmissions(assignmentId: string, request: BatchReviewRequest): Promise<BatchReviewResponse> {
  const { data } = await http.post<ApiEnvelope<BatchReviewResponse>>(
    `/api/v1/assignments/${encodeURIComponent(assignmentId)}/batch-review`,
    request,
  )
  return unwrapApiData(data, '批量评分失败')
}

export async function exportAssignmentGrades(assignmentId: string): Promise<void> {
  const url = `${API_BASE_URL || ''}/api/v1/assignments/${encodeURIComponent(assignmentId)}/export`.replace(/^\/+/, '/')
  const fullUrl = url.startsWith('http') ? url : window.location.origin + url

  const token = getStoredAccessToken()
  const headers: Record<string, string> = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
    headers['x-aigc-token'] = token
  }

  const response = await fetch(fullUrl, { headers })
  if (!response.ok) {
    throw new Error('导出成绩失败')
  }

  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition') || ''
  let filename = `assignment-${assignmentId}-grades.csv`
  const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
  if (match && match[1]) {
    filename = match[1].replace(/['"]/g, '')
  }

  const urlBlob = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = urlBlob
  a.download = decodeURIComponent(filename)
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(urlBlob)
}
