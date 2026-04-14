import type { LGraph, LGraphCanvas } from '@comfyorg/litegraph'
import { CANVAS_GRAPH_SCHEMA_VERSION, type GraphState } from '@/lib/graph/schema'

const DEFAULT_VIEWPORT: GraphState['viewport'] = { offset: [0, 0], scale: 1 }
const MIN_SCALE = 0.25
const MAX_SCALE = 4
const MAX_OFFSET = 20000

function sanitizeNumber(value: unknown, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function sanitizeViewport(viewport: GraphState['viewport'] | undefined): GraphState['viewport'] {
  const rawOffsetX = sanitizeNumber(viewport?.offset?.[0], DEFAULT_VIEWPORT.offset[0])
  const rawOffsetY = sanitizeNumber(viewport?.offset?.[1], DEFAULT_VIEWPORT.offset[1])
  const rawScale = sanitizeNumber(viewport?.scale, DEFAULT_VIEWPORT.scale)
  const scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, rawScale))
  const offsetX = Math.min(MAX_OFFSET, Math.max(-MAX_OFFSET, rawOffsetX))
  const offsetY = Math.min(MAX_OFFSET, Math.max(-MAX_OFFSET, rawOffsetY))
  return { offset: [offsetX, offsetY], scale }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function graphToState(graph: LGraph, canvas: LGraphCanvas): GraphState {
  const snapshot = graph.asSerialisable({ sortNodes: true })
  const state = canvas.ds.state
  return {
    schemaVersion: CANVAS_GRAPH_SCHEMA_VERSION,
    updatedAt: Date.now(),
    graph: snapshot,
    viewport: {
      offset: [state.offset[0], state.offset[1]],
      scale: state.scale,
    },
  }
}

export function applyState(graph: LGraph, canvas: LGraphCanvas, state: GraphState) {
  const viewport = sanitizeViewport(state.viewport)
  graph.configure(state.graph)
  canvas.ds.offset = [viewport.offset[0], viewport.offset[1]]
  canvas.ds.scale = viewport.scale
  canvas.setDirty(true, true)
}

export function parseImportedState(input: string): GraphState {
  const raw: unknown = JSON.parse(input)
  if (!isRecord(raw)) throw new Error('JSON 结构无效')

  if ('graph' in raw && isRecord(raw.graph)) {
    const viewport = isRecord(raw.viewport) ? raw.viewport : undefined
    const offsetRaw = viewport && Array.isArray(viewport.offset) ? viewport.offset : undefined
    const offset: [number, number] =
      offsetRaw && offsetRaw.length >= 2 && typeof offsetRaw[0] === 'number' && typeof offsetRaw[1] === 'number'
        ? [offsetRaw[0], offsetRaw[1]]
        : DEFAULT_VIEWPORT.offset
    const scale = viewport && typeof viewport.scale === 'number' ? viewport.scale : DEFAULT_VIEWPORT.scale

    const sanitizedViewport = sanitizeViewport({ offset, scale })

    return {
      schemaVersion: typeof raw.schemaVersion === 'number' ? raw.schemaVersion : CANVAS_GRAPH_SCHEMA_VERSION,
      updatedAt: Date.now(),
      graph: raw.graph as unknown as GraphState['graph'],
      viewport: sanitizedViewport,
    }
  }

  if ('nodes' in raw || 'links' in raw || 'groups' in raw) {
    return {
      schemaVersion: CANVAS_GRAPH_SCHEMA_VERSION,
      updatedAt: Date.now(),
      graph: raw as unknown as GraphState['graph'],
      viewport: DEFAULT_VIEWPORT,
    }
  }

  throw new Error('不支持的画布 JSON 格式')
}
