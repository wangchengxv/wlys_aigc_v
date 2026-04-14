import { create } from 'zustand'
import { listCanvasRemote, saveCanvasRemote } from '@/lib/graph/canvasRemoteApi'
import type { GraphState } from '@/lib/graph/schema'

const STORAGE_KEY = 'aigc-canvas-graph'
const REMOTE_ID_KEY = 'aigc-canvas-remote-id'

type CanvasGraphStore = {
  state: GraphState | null
  setState: (next: GraphState | null) => void
  load: () => GraphState | null
  syncFromRemote: () => Promise<GraphState | null>
  clear: () => void
}

export const useCanvasGraphStore = create<CanvasGraphStore>((set, get) => ({
  state: null,
  setState: (next) => {
    set({ state: next })
    try {
      if (next) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
        const remoteId = localStorage.getItem(REMOTE_ID_KEY) || undefined
        void saveCanvasRemote(next, { id: remoteId }).then((saved) => {
          localStorage.setItem(REMOTE_ID_KEY, saved.id)
        }).catch(() => {
          /* ignore remote sync failures */
        })
      } else {
        localStorage.removeItem(STORAGE_KEY)
        localStorage.removeItem(REMOTE_ID_KEY)
      }
    } catch {
      /* ignore */
    }
  },
  load: () => {
    const cached = get().state
    if (cached) return cached
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return null
      const parsed = JSON.parse(raw) as GraphState
      set({ state: parsed })
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
        graph: latest.graph,
        viewport: latest.viewport ?? { offset: [0, 0], scale: 1 },
      }
      set({ state: synced })
      localStorage.setItem(STORAGE_KEY, JSON.stringify(synced))
      localStorage.setItem(REMOTE_ID_KEY, latest.id)
      return synced
    } catch {
      return null
    }
  },
  clear: () => get().setState(null),
}))
