import { create } from 'zustand'
import { deleteTask, generateContent, getHistory } from '@/api'
import type { GenerateMode, GenerateRequest, GenerateResponse } from '@/types'

const FAVORITE_KEY = 'aigc_favorites_v1'

function readFavorites(): string[] {
  const raw = localStorage.getItem(FAVORITE_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as string[]
  } catch {
    return []
  }
}

function writeFavorites(taskIds: string[]) {
  localStorage.setItem(FAVORITE_KEY, JSON.stringify(taskIds))
}

type GenerationState = {
  loading: boolean
  tasks: GenerateResponse[]
  currentTask: GenerateResponse | null
  favorites: string[]
  generate: (req: GenerateRequest) => Promise<GenerateResponse>
  loadHistory: (page?: number, pageSize?: number, mode?: GenerateMode | 'all') => Promise<{ list: GenerateResponse[]; total: number }>
  toggleFavorite: (taskId: string) => void
  clearCurrent: () => void
  setCurrentTask: (task: GenerateResponse | null) => void
  removeTask: (taskId: string) => Promise<void>
}

export const useGenerationStore = create<GenerationState>((set, get) => ({
  loading: false,
  tasks: [],
  currentTask: null,
  favorites: readFavorites(),

  generate: async (req) => {
    set({ loading: true })
    try {
      const res = await generateContent(req)
      set((s) => ({
        currentTask: res,
        tasks: [res, ...s.tasks.filter((t) => t.taskId !== res.taskId)],
      }))
      return res
    } finally {
      set({ loading: false })
    }
  },

  loadHistory: async (page = 1, pageSize = 8, mode: GenerateMode | 'all' = 'all') => {
    const data = await getHistory({ page, pageSize, mode })
    set({ tasks: data.list })
    return data
  },

  toggleFavorite: (taskId) => {
    const { favorites } = get()
    const has = favorites.includes(taskId)
    const next = has ? favorites.filter((id) => id !== taskId) : [...favorites, taskId]
    writeFavorites(next)
    set({ favorites: next })
  },

  clearCurrent: () => set({ currentTask: null }),

  setCurrentTask: (task) => set({ currentTask: task }),

  removeTask: async (taskId) => {
    await deleteTask(taskId)
    set((s) => ({
      tasks: s.tasks.filter((item) => item.taskId !== taskId),
      currentTask: s.currentTask?.taskId === taskId ? null : s.currentTask,
    }))
  },
}))
