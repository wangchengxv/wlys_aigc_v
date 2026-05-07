import axios, { isAxiosError } from 'axios'
import type {
  BatchModelsImportRequest,
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ConnectionConfigUpdateRequest,
  ConnectionTestResponse,
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
  PresetModelListResponse,
  ProviderCatalogListResponse,
  QuickConnectionRequest,
  ScriptDocumentPayload,
  ScriptProjectAggregate,
  ScriptProjectCreateRequest,
  ScriptProjectSummary,
  ScriptProjectUploadRequest,
  ScriptRevision,
  StoryboardShot,
  UpdateAssetRequest,
  UpdateScriptRequest,
  VideoSegmentTask,
  VideoModelOptions,
} from '@/types'

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

export async function listScriptProjects(): Promise<ScriptProjectSummary[]> {
  requireScriptApi()
  const { data } = await http.get<ApiEnvelope<ScriptProjectSummary[]>>('/api/v1/script-projects')
  return unwrapApiData(data, '获取剧本项目列表失败')
}

export async function createScriptProject(payload: ScriptProjectCreateRequest): Promise<ScriptProjectAggregate> {
  requireScriptApi()
  const { data } = await http.post<ApiEnvelope<ScriptProjectAggregate>>('/api/v1/script-projects', payload)
  return unwrapApiData(data, '创建剧本项目失败')
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
  return unwrapApiData(data, '获取剧本项目详情失败')
}

export async function deleteScriptProject(projectId: string): Promise<void> {
  requireScriptApi()
  const path = `/api/v1/script-projects/${encodeURIComponent(projectId)}`
  try {
    const { data } = await http.delete<ApiEnvelope<null>>(path)
    unwrapApiVoid(data, '删除剧本项目失败')
  } catch (e) {
    throw enhanceAxiosError(e)
  }
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
