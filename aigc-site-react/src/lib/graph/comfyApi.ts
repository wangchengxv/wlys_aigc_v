import type { GraphState } from '@/lib/graph/schema'

type SerialNode = {
  id: number | string
  type?: string
  inputs?: Array<{ name?: string; link?: number | null }>
  widgets_values?: unknown[]
}

type LinkTuple = [number, number | string, number, number | string, number, string?]

type PromptNodePayload = {
  class_type: string
  inputs: Record<string, unknown>
}

/** 规范化 Comfy 根地址，避免 fetch 收到非法 URL（会报 “The string did not match the expected pattern”） */
function resolveComfyBase(): string {
  const raw = import.meta.env.VITE_COMFY_BASE_URL
  if (raw == null || String(raw).trim() === '') return '/api/comfy'
  const t = String(raw).trim().replace(/\/$/, '')
  if (t.startsWith('/')) return t === '' ? '/api/comfy' : t
  try {
    return new URL(t).toString().replace(/\/$/, '')
  } catch {
    try {
      return new URL(`http://${t}`).toString().replace(/\/$/, '')
    } catch {
      return '/api/comfy'
    }
  }
}

const COMFY_BASE = resolveComfyBase()

function toLinkMap(links: unknown): Map<number, LinkTuple> {
  const map = new Map<number, LinkTuple>()
  if (!Array.isArray(links)) return map
  for (const item of links) {
    if (!Array.isArray(item) || item.length < 5) continue
    const [id, fromNode, fromSlot, toNode, toSlot, type] = item
    if (typeof id !== 'number') continue
    map.set(id, [id, fromNode as number | string, Number(fromSlot), toNode as number | string, Number(toSlot), typeof type === 'string' ? type : undefined])
  }
  return map
}

function nodeClassType(nodeType?: string): string {
  if (nodeType === 'aigc/prompt') return 'AIGCTextPrompt'
  if (nodeType === 'aigc/render') return 'AIGCRender'
  return 'AIGCCustomNode'
}

export function buildPromptFromGraph(state: GraphState): Record<string, PromptNodePayload> {
  const prompt: Record<string, PromptNodePayload> = {}
  const nodes = Array.isArray((state.graph as { nodes?: unknown }).nodes) ? ((state.graph as { nodes: unknown[] }).nodes as SerialNode[]) : []
  const linkMap = toLinkMap((state.graph as { links?: unknown }).links)

  for (const node of nodes) {
    const id = String(node.id)
    const inputs: Record<string, unknown> = {}
    if (node.type === 'aigc/prompt') {
      inputs.text = typeof node.widgets_values?.[0] === 'string' ? node.widgets_values[0] : ''
    }
    if (Array.isArray(node.inputs)) {
      for (const slot of node.inputs) {
        if (!slot?.name || typeof slot.link !== 'number') continue
        const link = linkMap.get(slot.link)
        if (!link) continue
        inputs[slot.name] = [String(link[1]), link[2]]
      }
    }
    prompt[id] = {
      class_type: nodeClassType(node.type),
      inputs,
    }
  }
  return prompt
}

async function comfyFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const base = COMFY_BASE.endsWith('/') ? COMFY_BASE.slice(0, -1) : COMFY_BASE
  const p = path.startsWith('/') ? path : `/${path}`
  const url = base.startsWith('http') ? `${base}${p}` : `${base}${p}`
  const response = await fetch(url, init)
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Comfy 请求失败: ${response.status}`)
  }
  return (await response.json()) as T
}

export async function fetchComfyObjectInfo(): Promise<Record<string, unknown>> {
  return comfyFetch<Record<string, unknown>>('/object_info')
}

export async function queueComfyPrompt(prompt: Record<string, PromptNodePayload>): Promise<{ prompt_id: string }> {
  return comfyFetch<{ prompt_id: string }>('/prompt', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      client_id: `aigc_${Date.now().toString(36)}`,
      prompt,
    }),
  })
}

export async function fetchComfyHistory(promptId: string): Promise<Record<string, unknown>> {
  return comfyFetch<Record<string, unknown>>(`/history/${encodeURIComponent(promptId)}`)
}

type PollComfyHistoryOptions = {
  intervalMs?: number
  maxAttempts?: number
}

export async function pollComfyHistory(
  promptId: string,
  options?: PollComfyHistoryOptions,
): Promise<{ history: Record<string, unknown> | null; completed: boolean }> {
  const intervalMs = options?.intervalMs ?? 2000
  const maxAttempts = options?.maxAttempts ?? 30
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const history = await fetchComfyHistory(promptId)
    const completed = Boolean(history?.[promptId])
    if (completed) {
      return { history, completed: true }
    }
    if (attempt < maxAttempts - 1) {
      await new Promise((resolve) => window.setTimeout(resolve, intervalMs))
    }
  }
  return { history: null, completed: false }
}
