export type GenerateMode = 'text' | 'image' | 'both' | 'video'

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
