import { create } from 'zustand'
import { listCanvasRemote, saveCanvasRemote } from '@/lib/graph/canvasRemoteApi'
import type { GraphState } from '@/lib/graph/schema'

const STORAGE_KEY = 'aigc-canvas-graph'
const REMOTE_ID_KEY = 'aigc-canvas-remote-id'

export type CanvasRemoteStatus = 'idle' | 'saving' | 'saved' | 'error'

type CanvasGraphStore = {
  state: GraphState | null
  remoteId: string | null
  remoteStatus: CanvasRemoteStatus
  remoteError: string | null
  setState: (next: GraphState | null) => void
  load: () => GraphState | null
  syncFromRemote: () => Promise<GraphState | null>
  clear: () => void
}

export const useCanvasGraphStore = create<CanvasGraphStore>((set, get) => ({
  state: null,
  remoteId: null,
  remoteStatus: 'idle',
  remoteError: null,
  setState: (next) => {
    set((current) => ({
      state: next,
      remoteStatus: next ? 'saving' : 'idle',
      remoteError: next ? null : current.remoteError,
    }))
    try {
      if (next) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
        const remoteId = get().remoteId ?? localStorage.getItem(REMOTE_ID_KEY) ?? undefined
        void saveCanvasRemote(next, { id: remoteId, projectId: next.projectId ?? undefined, title: next.title ?? undefined })
          .then((saved) => {
            localStorage.setItem(REMOTE_ID_KEY, saved.id)
            set({ remoteId: saved.id, remoteStatus: 'saved', remoteError: null })
          })
          .catch((error) => {
            set({
              remoteStatus: 'error',
              remoteError: error instanceof Error ? error.message : '远端保存失败，已退回本地缓存',
            })
          })
      } else {
        localStorage.removeItem(STORAGE_KEY)
        localStorage.removeItem(REMOTE_ID_KEY)
        set({ remoteId: null, remoteStatus: 'idle', remoteError: null })
      }
    } catch (error) {
      if (next) {
        set({
          remoteStatus: 'error',
          remoteError: error instanceof Error ? error.message : '本地缓存失败',
        })
      }
    }
  },
  load: () => {
    const cached = get().state
    if (cached) return cached
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return null
      const parsed = JSON.parse(raw) as GraphState
      set({
        state: parsed,
        remoteId: localStorage.getItem(REMOTE_ID_KEY),
        remoteStatus: 'saved',
        remoteError: null,
      })
      return parsed
    } catch {
      return null
    }
  },
  syncFromRemote: async () => {
    try {
      const listed = await listCanvasRemote(1, 1)
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
        remoteId: latest.id,
        remoteStatus: 'saved',
        remoteError: null,
      })
      localStorage.setItem(STORAGE_KEY, JSON.stringify(synced))
      localStorage.setItem(REMOTE_ID_KEY, latest.id)
      return synced
    } catch {
      return null
    }
  },
  clear: () => get().setState(null),
}))
