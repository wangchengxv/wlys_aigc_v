import axios from 'axios'
import type {
  ConnectionConfig,
  ConnectionConfigCreateRequest,
  ConnectionConfigUpdateRequest,
  GenerateRequest,
  GenerateResponse,
  HistoryQuery,
  ImageModelOptions,
  ModelConfig,
  ModelConfigCreateRequest,
  ModelConfigUpdateRequest,
  PagedTasks,
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

function unwrapApiData<T>(payload: ApiEnvelope<T>, fallbackMessage: string): T {
  if (!payload || payload.code !== 200 || payload.data == null) {
    throw new Error(payload?.message || fallbackMessage)
  }
  return payload.data
}

const http = axios.create({
  baseURL: API_BASE_URL || undefined,
  timeout: 130000,
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
