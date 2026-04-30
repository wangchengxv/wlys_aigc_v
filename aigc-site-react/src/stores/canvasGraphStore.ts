import { create } from 'zustand'
import { listCanvasRemote, saveCanvasRemote } from '@/lib/graph/canvasRemoteApi'
import type { GraphState } from '@/lib/graph/schema'

const STORAGE_KEY_PREFIX = 'aigc-canvas-graph'
const LEGACY_STORAGE_KEY = 'aigc-canvas-graph'
const REMOTE_ID_KEY_PREFIX = 'aigc-canvas-remote-id'
const LEGACY_REMOTE_ID_KEY = 'aigc-canvas-remote-id'
const REMOTE_SAVE_DEBOUNCE_MS = 800

export type CanvasRemoteStatus = 'idle' | 'saving' | 'saved' | 'error'
type CanvasContext = { projectId?: string | null }

function normalizeProjectId(projectId?: string | null) {
  const trimmed = projectId?.trim()
  return trimmed || null
}

function storageKeyFor(projectId?: string | null) {
  return `${STORAGE_KEY_PREFIX}:${normalizeProjectId(projectId) ?? '__unbound__'}`
}

function remoteIdKeyFor(projectId?: string | null) {
  return `${REMOTE_ID_KEY_PREFIX}:${normalizeProjectId(projectId) ?? '__unbound__'}`
}

type CanvasGraphStore = {
  state: GraphState | null
  contextProjectId: string | null
  remoteId: string | null
  remoteStatus: CanvasRemoteStatus
  remoteError: string | null
  remoteSavedAt: number | null
  setState: (next: GraphState | null) => void
  load: (context?: CanvasContext) => GraphState | null
  syncFromRemote: (context?: CanvasContext) => Promise<GraphState | null>
  clear: (context?: CanvasContext) => void
}

let remoteSaveTimer: number | null = null
let remoteSaveInFlight = false
let pendingSnapshot: GraphState | null = null
let pendingContextProjectId: string | null = null

export const useCanvasGraphStore = create<CanvasGraphStore>((set, get) => ({
  state: null,
  contextProjectId: null,
  remoteId: null,
  remoteStatus: 'idle',
  remoteError: null,
  remoteSavedAt: null,
  setState: (next) => {
    if (next === null) {
      set((current) => ({
        state: null,
        remoteStatus: 'idle',
        remoteError: null,
        remoteSavedAt: current.remoteSavedAt,
      }))
      return
    }
    const contextProjectId = normalizeProjectId(next.projectId)
    const storageKey = storageKeyFor(contextProjectId)
    set((current) => ({
      state: next,
      contextProjectId,
      remoteStatus: next ? 'saving' : 'idle',
      remoteError: next ? null : current.remoteError,
    }))
    try {
      localStorage.setItem(storageKey, JSON.stringify(next))
      pendingSnapshot = next
      pendingContextProjectId = contextProjectId

      if (remoteSaveTimer != null) {
        window.clearTimeout(remoteSaveTimer)
      }
      remoteSaveTimer = window.setTimeout(() => {
        remoteSaveTimer = null
        const flush = async () => {
          if (remoteSaveInFlight || !pendingSnapshot) return
          const snapshot = pendingSnapshot
          const activeContext = pendingContextProjectId
          pendingSnapshot = null
          pendingContextProjectId = null
          remoteSaveInFlight = true
          set({ remoteStatus: 'saving', remoteError: null })
          try {
            const activeRemoteIdKey = remoteIdKeyFor(activeContext)
            const remoteId = get().remoteId ?? localStorage.getItem(activeRemoteIdKey) ?? localStorage.getItem(LEGACY_REMOTE_ID_KEY) ?? undefined
            const saved = await saveCanvasRemote(snapshot, {
              id: remoteId,
              projectId: snapshot.projectId ?? undefined,
              title: snapshot.title ?? undefined,
            })
            localStorage.setItem(activeRemoteIdKey, saved.id)
            set({
              remoteId: saved.id,
              remoteStatus: 'saved',
              remoteError: null,
              remoteSavedAt: Date.now(),
            })
          } catch (error) {
            set({
              remoteStatus: 'error',
              remoteError: error instanceof Error ? error.message : '远端保存失败，已退回本地缓存',
            })
          } finally {
            remoteSaveInFlight = false
            if (pendingSnapshot) {
              void flush()
            }
          }
        }
        void flush()
      }, REMOTE_SAVE_DEBOUNCE_MS)
    } catch (error) {
      if (next) {
        set({
          remoteStatus: 'error',
          remoteError: error instanceof Error ? error.message : '本地缓存失败',
        })
      }
    }
  },
  load: (context) => {
    const contextProjectId = normalizeProjectId(context?.projectId)
    const currentState = get().state
    if (currentState && normalizeProjectId(currentState.projectId) === contextProjectId) return currentState
    try {
      const storageKey = storageKeyFor(contextProjectId)
      const remoteIdKey = remoteIdKeyFor(contextProjectId)
      const raw = localStorage.getItem(storageKey) ?? (contextProjectId == null ? localStorage.getItem(LEGACY_STORAGE_KEY) : null)
      if (!raw) return null
      const parsed = JSON.parse(raw) as GraphState
      set({
        state: parsed,
        contextProjectId,
        remoteId: localStorage.getItem(remoteIdKey) ?? (contextProjectId == null ? localStorage.getItem(LEGACY_REMOTE_ID_KEY) : null),
        remoteStatus: 'saved',
        remoteError: null,
      })
      return parsed
    } catch {
      return null
    }
  },
  syncFromRemote: async (context) => {
    const contextProjectId = normalizeProjectId(context?.projectId)
    const storageKey = storageKeyFor(contextProjectId)
    const remoteIdKey = remoteIdKeyFor(contextProjectId)
    try {
      const listed = await listCanvasRemote(1, 1, { projectId: contextProjectId })
      const latest = listed.list?.[0]
      if (!latest?.graph) return null
      const synced: GraphState = {
        schemaVersion: 1,
        updatedAt: latest.updatedAt ? new Date(latest.updatedAt).getTime() : Date.now(),
        title: latest.title ?? '',
        projectId: latest.projectId,
        graph: latest.graph,
        viewport: latest.viewport ?? { offset: [0, 0], scale: 1 },
      }
      set({
        state: synced,
        contextProjectId,
        remoteId: latest.id,
        remoteStatus: 'saved',
        remoteError: null,
        remoteSavedAt: Date.now(),
      })
      localStorage.setItem(storageKey, JSON.stringify(synced))
      localStorage.setItem(remoteIdKey, latest.id)
      return synced
    } catch {
      return null
    }
  },
  clear: (context) => {
    const contextProjectId = normalizeProjectId(context?.projectId ?? get().contextProjectId)
    const storageKey = storageKeyFor(contextProjectId)
    const remoteIdKey = remoteIdKeyFor(contextProjectId)
    localStorage.removeItem(storageKey)
    localStorage.removeItem(remoteIdKey)
    if (contextProjectId == null) {
      localStorage.removeItem(LEGACY_STORAGE_KEY)
      localStorage.removeItem(LEGACY_REMOTE_ID_KEY)
    }
    pendingSnapshot = null
    pendingContextProjectId = null
    if (remoteSaveTimer != null) {
      window.clearTimeout(remoteSaveTimer)
      remoteSaveTimer = null
    }
    set({
      state: null,
      contextProjectId,
      remoteId: null,
      remoteStatus: 'idle',
      remoteError: null,
    })
  },
}))
