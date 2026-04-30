import type { GraphState } from '@/lib/graph/schema'
import { getStoredAccessToken } from '@/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const ACCESS_TOKEN = import.meta.env.VITE_AIGC_ACCESS_TOKEN || 'dev-local-token'
const CLIENT_USER_ID_KEY = 'aigc_client_user_id'

type ApiEnvelope<T> = {
  code: number
  message: string
  data: T | null
}

type CanvasRemoteRecord = {
  id: string
  projectId: string | null
  title: string | null
  graph: GraphState['graph']
  viewport: GraphState['viewport'] | null
  createdAt: string
  updatedAt: string
}

type PagedResult<T> = {
  list: T[]
  total: number
}
const UNBOUND_PROJECT_KEY = '__unbound__'

function getOrCreateClientUserId() {
  const existing = localStorage.getItem(CLIENT_USER_ID_KEY)
  if (existing && existing.trim()) return existing
  const created = `u_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
  localStorage.setItem(CLIENT_USER_ID_KEY, created)
  return created
}

function getHeaders() {
  const token = getStoredAccessToken() || ACCESS_TOKEN.trim()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
    'x-aigc-token': token,
  }
  if (token === ACCESS_TOKEN.trim()) {
    headers['x-user-id'] = getOrCreateClientUserId()
  }
  return headers
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, init)
  const payload = (await response.json()) as ApiEnvelope<T>
  if (!response.ok || payload.code !== 200 || payload.data == null) {
    throw new Error(payload?.message || `请求失败: ${response.status}`)
  }
  return payload.data
}

export async function listCanvasRemote(
  page = 1,
  pageSize = 1,
  options?: { projectId?: string | null },
): Promise<PagedResult<CanvasRemoteRecord>> {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  })
  const projectId = options?.projectId?.trim()
  if (projectId) {
    params.set('projectId', projectId)
  } else if (options && options.projectId === null) {
    params.set('projectId', UNBOUND_PROJECT_KEY)
  }
  return requestJson<PagedResult<CanvasRemoteRecord>>(`/api/v1/canvas?${params.toString()}`, {
    method: 'GET',
    headers: getHeaders(),
  })
}

export async function loadCanvasRemote(id: string): Promise<CanvasRemoteRecord> {
  return requestJson<CanvasRemoteRecord>(`/api/v1/canvas/${encodeURIComponent(id)}`, {
    method: 'GET',
    headers: getHeaders(),
  })
}

export async function saveCanvasRemote(
  state: GraphState,
  options?: { id?: string; projectId?: string; title?: string },
): Promise<CanvasRemoteRecord> {
  return requestJson<CanvasRemoteRecord>('/api/v1/canvas', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({
      id: options?.id,
      projectId: options?.projectId ?? null,
      title: options?.title ?? '默认无限画布',
      graph: state.graph,
      viewport: state.viewport,
    }),
  })
}
