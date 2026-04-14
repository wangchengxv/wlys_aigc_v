import axios, { AxiosHeaders, isAxiosError } from 'axios'
import type {
  ArtDirectionResponse,
  BatchModelsImportRequest,
  BatchVisualPromptResponse,
  ApplyStoryboardFirstFrameRequest,
  AppendScriptPreviewRequest,
  AppendScriptPreviewResponse,
  AssetGenerationHistoryItem,
  AssetHistoryType,
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ConnectionConfigUpdateRequest,
  ConnectionTestResponse,
  GenerateGroupSceneRequest,
  GroupSceneResponse,
  ExtractedAsset,
  GenerateRequest,
  GenerateResponse,
  HistoryQuery,
  ImageModelOptions,
  KeyframeRecord,
  ModelConfig,
  ModelConfigCreateRequest,
  ModelConfigUpdateRequest,
  ModelProbeResponse,
  PagedTasks,
  PipelineStatus,
  PromptTemplateCatalogItem,
  PresetModelListResponse,
  ProviderCatalogListResponse,
  QuickConnectionRequest,
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
  ThreeViewResponse,
  StoryboardShot,
  TurnaroundImageResponse,
  TurnaroundPlanResponse,
  UpdateAssetRequest,
  UpdateShotRequest,
  UpdateScriptRequest,
  VisualPromptResponse,
  VideoSegmentTask,
  VideoModelOptions,
  WorkflowModelSettings,
  WorkflowModelSettingsUpdateRequest,
} from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const USE_MOCK = !API_BASE_URL
const STORAGE_KEY = 'aigc_tasks_v1'
const DEFAULT_MODEL = 'doubao-seedream-5-0-260128'
const DEFAULT_VIDEO_MODEL = 'doubao-seedance-1-5-pro-251215'
const ACCESS_TOKEN = import.meta.env.VITE_AIGC_ACCESS_TOKEN || 'dev-local-token'
const CLIENT_USER_ID_KEY = 'aigc_client_user_id'

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

http.interceptors.request.use((config) => {
  if (!USE_MOCK) {
    const token = ACCESS_TOKEN.trim()
    const userId = getOrCreateClientUserId()
    const headers = AxiosHeaders.from(config.headers ?? {})
    headers.set('Authorization', `Bearer ${token}`)
    headers.set('x-aigc-token', token)
    headers.set('x-user-id', userId)
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

const MOCK_PRESET_MODELS: PresetModelListResponse = {
  providers: ['openai', 'anthropic', 'deepseek', 'qwen', 'ark', 'onelinkai', 'moark'],
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
    { provider: 'onelinkai', modelName: 'MiniMax-M2.1', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI MiniMax M2.1', capabilities: ['video'] },
    { provider: 'onelinkai', modelName: 'viduq3-turbo', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Vidu Q3 Turbo', capabilities: ['video'] },
    { provider: 'onelinkai', modelName: 'viduq3-pro', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Vidu Q3 Pro', capabilities: ['video'] },
    { provider: 'onelinkai', modelName: 'viduq2', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Vidu Q2', capabilities: ['video'] },
    { provider: 'onelinkai', modelName: 'viduq1', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Vidu Q1', capabilities: ['video'] },
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
        'MiniMax-M2.1',
        'viduq3-turbo',
        'viduq3-pro',
        'viduq2',
        'viduq1',
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

export async function getScriptProjectPipelineStatus(projectId: string): Promise<PipelineStatus> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<PipelineStatus>>(`/api/v1/script-projects/${projectId}/pipeline-status`)
  return unwrapApiData(data, '获取流水线状态失败')
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
