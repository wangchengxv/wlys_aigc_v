import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { deleteTask, generateContent, getHistory } from '../services/api'
import type { GenerateMode, GenerateRequest, GenerateResponse } from '../types'

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

export const useGenerationStore = defineStore('generation', () => {
  const loading = ref(false)
  const tasks = ref<GenerateResponse[]>([])
  const currentTask = ref<GenerateResponse | null>(null)
  const favorites = ref<string[]>(readFavorites())

  const favoriteSet = computed(() => new Set(favorites.value))

  async function generate(req: GenerateRequest) {
    loading.value = true
    try {
      const res = await generateContent(req)
      currentTask.value = res
      tasks.value = [res, ...tasks.value.filter((t) => t.taskId !== res.taskId)]
      return res
    } finally {
      loading.value = false
    }
  }

  async function loadHistory(page = 1, pageSize = 8, mode: GenerateMode | 'all' = 'all') {
    const data = await getHistory({ page, pageSize, mode })
    tasks.value = data.list
    return data
  }

  function toggleFavorite(taskId: string) {
    const has = favoriteSet.value.has(taskId)
    favorites.value = has ? favorites.value.filter((id) => id !== taskId) : [...favorites.value, taskId]
    writeFavorites(favorites.value)
  }

  function clearCurrent() {
    currentTask.value = null
  }

  async function removeTask(taskId: string) {
    await deleteTask(taskId)
    tasks.value = tasks.value.filter((item) => item.taskId !== taskId)
    if (currentTask.value?.taskId === taskId) {
      currentTask.value = null
    }
  }

  return {
    loading,
    tasks,
    currentTask,
    favorites,
    favoriteSet,
    generate,
    loadHistory,
    toggleFavorite,
    clearCurrent,
    removeTask,
  }
})
