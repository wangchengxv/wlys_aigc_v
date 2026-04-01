import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getPresetById } from '@/data/videoStylePresets'
import type {
  GlobalAspectRatio,
  GlobalCreationMode,
  GlobalModelStrategy,
  GlobalScriptType,
  GlobalStoryboardLayout,
  GlobalVisualStyleMode,
} from '@/types'

const STORAGE_KEY = 'aigc-global-settings'

type Snapshot = {
  aspectRatio: GlobalAspectRatio
  scriptType: GlobalScriptType
  modelStrategy: GlobalModelStrategy
  creationMode: GlobalCreationMode
  storyboardLayout: GlobalStoryboardLayout
  visualStyleMode: GlobalVisualStyleMode
  visualStylePresetId: string | null
  customVisualStyle: string
  visualStyleLongTextMode: boolean
  targetDurationSec: number
}

const DEFAULT: Snapshot = {
  aspectRatio: '16:9',
  scriptType: '剧情演绎',
  modelStrategy: '省钱优先',
  creationMode: '生视频模式',
  storyboardLayout: '单',
  visualStyleMode: 'custom',
  visualStylePresetId: null,
  customVisualStyle: '电影感写实',
  visualStyleLongTextMode: false,
  targetDurationSec: 15,
}

function loadPartial(): Partial<Snapshot> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const out: Partial<Snapshot> = {}
    if (parsed.aspectRatio && ['16:9', '9:16', '4:3', '3:4', '1:1', '21:9'].includes(String(parsed.aspectRatio))) {
      out.aspectRatio = parsed.aspectRatio as GlobalAspectRatio
    }
    if (parsed.scriptType && ['剧情演绎', '真人解说'].includes(String(parsed.scriptType))) {
      out.scriptType = parsed.scriptType as GlobalScriptType
    }
    if (parsed.modelStrategy && ['省钱优先', '画质优先'].includes(String(parsed.modelStrategy))) {
      out.modelStrategy = parsed.modelStrategy as GlobalModelStrategy
    }
    if (parsed.creationMode && ['生视频模式', '多参数生视频'].includes(String(parsed.creationMode))) {
      out.creationMode = parsed.creationMode as GlobalCreationMode
    }
    if (parsed.storyboardLayout && ['单', '九宫格机位'].includes(String(parsed.storyboardLayout))) {
      out.storyboardLayout = parsed.storyboardLayout as GlobalStoryboardLayout
    }
    if (parsed.visualStyleMode && ['preset', 'custom'].includes(String(parsed.visualStyleMode))) {
      out.visualStyleMode = parsed.visualStyleMode as GlobalVisualStyleMode
    }
    if (parsed.visualStylePresetId === null) {
      out.visualStylePresetId = null
    } else if (typeof parsed.visualStylePresetId === 'string') {
      out.visualStylePresetId = getPresetById(parsed.visualStylePresetId) ? parsed.visualStylePresetId : null
    }
    if (typeof parsed.customVisualStyle === 'string') out.customVisualStyle = parsed.customVisualStyle
    if (typeof parsed.visualStyleLongTextMode === 'boolean') out.visualStyleLongTextMode = parsed.visualStyleLongTextMode
    const duration = Number(parsed.targetDurationSec)
    if (Number.isFinite(duration) && duration >= 1 && duration <= 600) out.targetDurationSec = Math.round(duration)
    return out
  } catch {
    return {}
  }
}

export const useGlobalSettingsStore = defineStore('global-settings', () => {
  const state = ref<Snapshot>({ ...DEFAULT })

  const snapshot = computed(() => state.value)

  function persist(next: Snapshot) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // ignore
    }
  }

  function init() {
    state.value = { ...DEFAULT, ...loadPartial() }
  }

  function patch(partial: Partial<Snapshot>) {
    state.value = { ...state.value, ...partial }
    persist(state.value)
  }

  function reset() {
    state.value = { ...DEFAULT }
    persist(state.value)
  }

  return { state, snapshot, init, patch, reset }
})
