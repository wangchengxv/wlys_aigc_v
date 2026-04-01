import axios, { isAxiosError } from 'axios'
import type {
  ApplyStoryboardFirstFrameRequest,
  AppendScriptPreviewRequest,
  AppendScriptPreviewResponse,
  ArtDirectionResponse,
  BatchModelsImportRequest,
  BatchVisualPromptResponse,
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ConnectionConfigUpdateRequest,
  ConnectionTestResponse,
  ExtractedAsset,
  GenerateRequest,
  GenerateGroupSceneRequest,
  GenerateResponse,
  GroupSceneResponse,
  HistoryQuery,
  ImageModelOptions,
  KeyframeRecord,
  ModelConfig,
  ModelConfigCreateRequest,
  ModelProbeResponse,
  ModelConfigUpdateRequest,
  PagedTasks,
  PipelineStatus,
  PresetModelListResponse,
  ProviderCatalogListResponse,
  QuickConnectionRequest,
  RewriteScriptApplyRequest,
  RewriteScriptPreviewRequest,
  RewriteScriptPreviewResponse,
  RouterApiKey,
  RouterLogPage,
  RouterRoutingConfig,
  RouterStats,
  ScriptRevision,
  ShotVisualPromptResponse,
  ScriptDocumentPayload,
  ScriptProjectAggregate,
  ScriptProjectCreateRequest,
  ScriptProjectSummary,
  ScriptProjectUploadRequest,
  StoryboardFirstFrameResponse,
  StoryboardImageResponse,
  StoryboardPanelCropResponse,
  StoryboardPlanResponse,
  StoryboardRewriteRequest,
  StoryboardShot,
  ThreeViewResponse,
  TurnaroundImageResponse,
  TurnaroundPlanResponse,
  UpdateShotRequest,
  UpdateAssetRequest,
  UpdateScriptRequest,
  VisualPromptResponse,
  VideoSegmentTask,
  VideoModelOptions,
} from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const USE_MOCK = !API_BASE_URL
const STORAGE_KEY = 'aigc_tasks_v1'
const DEFAULT_MODEL = 'doubao-seedream-5-0-260128'
const DEFAULT_VIDEO_MODEL = 'doubao-seedance-1-5-pro-251215'

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

/** 成功时 data 可为 null（如删除），仅校验 code */
function unwrapApiVoid(payload: ApiEnvelope<unknown> | undefined | null, fallbackMessage: string): void {
  if (payload == null) return
  if (typeof payload !== 'object' || Array.isArray(payload)) return
  const p = payload as ApiEnvelope<unknown>
  if (p.code !== 200) {
    throw new Error(p.message || fallbackMessage)
  }
}

function enhanceAxiosError(e: unknown): Error {
  if (isAxiosError(e) && e.response?.data && typeof e.response.data === 'object' && e.response.data !== null) {
    const body = e.response.data as { message?: string }
    if (typeof body.message === 'string' && body.message) {
      return new Error(body.message)
    }
  }
  return e instanceof Error ? e : new Error(String(e))
}

const http = axios.create({
  baseURL: API_BASE_URL || undefined,
  timeout: 1300000,
})

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

function normalizeTask(task: GenerateResponse): GenerateResponse {
  return {
    ...task,
    textResults: Array.isArray(task.textResults) ? task.textResults : [],
    imageResults: Array.isArray(task.imageResults) ? task.imageResults : [],
    videoResults: Array.isArray(task.videoResults) ? task.videoResults : [],
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

export async function getConnections(): Promise<ConnectionConfig[]> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<ConnectionConfig[]>>('/api/v1/connections')
    return unwrapApiData(data, '获取连接列表失败')
  }
  return []
}

export async function getPresetModels(): Promise<PresetModelListResponse> {
  const { data } = await http.get<ApiEnvelope<PresetModelListResponse>>('/api/v1/preset-models')
  return unwrapApiData(data, '获取预置模型列表失败')
}

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
  ],
}

export async function getProviderCatalog(): Promise<ProviderCatalogListResponse> {
  if (!USE_MOCK) {
    const { data } = await http.get<ApiEnvelope<ProviderCatalogListResponse>>('/api/v1/provider-catalog')
    return unwrapApiData(data, '获取服务商目录失败')
  }
  return MOCK_PROVIDER_CATALOG
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

export async function getRouterKeys(): Promise<RouterApiKey[]> {
  const { data } = await http.get<ApiEnvelope<RouterApiKey[]>>('/api/v1/router/keys')
  return unwrapApiData(data, '获取路由 API Key 失败')
}

export async function createRouterKey(name: string): Promise<RouterApiKey> {
  const { data } = await http.post<ApiEnvelope<RouterApiKey>>('/api/v1/router/keys', { name })
  return unwrapApiData(data, '创建路由 API Key 失败')
}

export async function toggleRouterKey(id: string, active: boolean): Promise<RouterApiKey> {
  const { data } = await http.patch<ApiEnvelope<RouterApiKey>>(`/api/v1/router/keys/${id}`, { active })
  return unwrapApiData(data, '更新路由 API Key 状态失败')
}

export async function deleteRouterKey(id: string): Promise<void> {
  await http.delete(`/api/v1/router/keys/${id}`)
}

export async function getRouterRouting(): Promise<RouterRoutingConfig> {
  const { data } = await http.get<ApiEnvelope<RouterRoutingConfig>>('/api/v1/router/routing')
  return unwrapApiData(data, '获取路由配置失败')
}

export async function updateRouterRouting(payload: RouterRoutingConfig): Promise<RouterRoutingConfig> {
  const { data } = await http.put<ApiEnvelope<RouterRoutingConfig>>('/api/v1/router/routing', payload)
  return unwrapApiData(data, '保存路由配置失败')
}

export async function getRouterLogs(params: {
  page: number
  pageSize: number
  status?: string
  connectionId?: string
  days?: number
}): Promise<RouterLogPage> {
  const { data } = await http.get<ApiEnvelope<RouterLogPage>>('/api/v1/router/logs', { params })
  return unwrapApiData(data, '获取路由日志失败')
}

export async function getRouterStats(): Promise<RouterStats> {
  const { data } = await http.get<ApiEnvelope<RouterStats>>('/api/v1/router/stats')
  return unwrapApiData(data, '获取路由统计失败')
}

export async function exportRouterConfig(): Promise<Record<string, unknown>> {
  const { data } = await http.get<ApiEnvelope<Record<string, unknown>>>('/api/v1/router/config/export')
  return unwrapApiData(data, '导出配置失败')
}

export async function importRouterConfig(payload: Record<string, unknown>): Promise<void> {
  await http.post('/api/v1/router/config/import', payload)
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
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>('/api/v1/script-projects', payload)
  return unwrapApiData(data, '创建剧本工程失败')
}

export async function uploadScriptProject(payload: ScriptProjectUploadRequest): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const formData = new FormData()
  formData.append('name', payload.name)
  formData.append('file', payload.file)
  if (payload.visualStyle) formData.append('visualStyle', payload.visualStyle)
  if (payload.aspectRatio) formData.append('aspectRatio', payload.aspectRatio)
  if (payload.targetDuration != null) formData.append('targetDuration', String(payload.targetDuration))
  if (payload.language) formData.append('language', payload.language)
  if (payload.explicitTextModel) formData.append('explicitTextModel', payload.explicitTextModel)
  if (payload.explicitImageModel) formData.append('explicitImageModel', payload.explicitImageModel)
  if (payload.explicitVideoModel) formData.append('explicitVideoModel', payload.explicitVideoModel)
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>('/api/v1/script-projects/upload', formData)
  return unwrapApiData(data, '上传剧本失败')
}

export async function getScriptProject(projectId: string): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ScriptProjectAggregate>>(`/api/v1/script-projects/${projectId}`)
  return unwrapApiData(data, '获取剧本工程详情失败')
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

export async function refineScriptProjectWithPrompt(projectId: string, briefPrompt: string): Promise<ScriptDocumentPayload> {
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

export async function getScriptProjectPipelineStatus(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/pipeline-status`)
  return unwrapApiData(data, '获取流水线状态失败')
}

export function resolveScriptFileUrl(fileId?: string | null): string {
  if (!fileId) return ''
  if (!API_BASE_URL) return `/api/v1/files/${fileId}`
  const normalized = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
  return `${normalized}/api/v1/files/${fileId}`
}
