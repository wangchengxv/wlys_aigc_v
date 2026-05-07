import { create } from 'zustand'
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

export interface GlobalSettingsSnapshot {
  aspectRatio: GlobalAspectRatio
  scriptType: GlobalScriptType
  modelStrategy: GlobalModelStrategy
  creationMode: GlobalCreationMode
  storyboardLayout: GlobalStoryboardLayout
  visualStyleMode: GlobalVisualStyleMode
  visualStylePresetId: string | null
  customVisualStyle: string
  /** 系统预设时：true 将完整示例提示词写入项目 visualStyle */
  visualStyleLongTextMode: boolean
  /** 新建剧本项目的目标时长（秒） */
  targetDurationSec: number
}

const DEFAULT: GlobalSettingsSnapshot = {
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

function isAspectRatio(v: unknown): v is GlobalAspectRatio {
  return (
    v === '16:9' ||
    v === '9:16' ||
    v === '4:3' ||
    v === '3:4' ||
    v === '1:1' ||
    v === '21:9'
  )
}

function isScriptType(v: unknown): v is GlobalScriptType {
  return v === '剧情演绎' || v === '真人解说'
}

function isModelStrategy(v: unknown): v is GlobalModelStrategy {
  return v === '省钱优先' || v === '画质优先'
}

function isCreationMode(v: unknown): v is GlobalCreationMode {
  return v === '生视频模式' || v === '多参数生视频'
}

function isStoryboardLayout(v: unknown): v is GlobalStoryboardLayout {
  return v === '单' || v === '九宫格机位'
}

function isVisualStyleMode(v: unknown): v is GlobalVisualStyleMode {
  return v === 'preset' || v === 'custom'
}

function parseTargetDurationSec(v: unknown): number | null {
  const n = typeof v === 'number' ? v : typeof v === 'string' ? Number(v) : NaN
  if (!Number.isFinite(n)) return null
  const int = Math.round(n)
  if (int < 1 || int > 600) return null
  return int
}

function loadPartial(): Partial<GlobalSettingsSnapshot> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const out: Partial<GlobalSettingsSnapshot> = {}
    if (isAspectRatio(parsed.aspectRatio)) out.aspectRatio = parsed.aspectRatio
    if (isScriptType(parsed.scriptType)) out.scriptType = parsed.scriptType
    if (isModelStrategy(parsed.modelStrategy)) out.modelStrategy = parsed.modelStrategy
    if (isCreationMode(parsed.creationMode)) out.creationMode = parsed.creationMode
    if (isStoryboardLayout(parsed.storyboardLayout)) out.storyboardLayout = parsed.storyboardLayout
    if (isVisualStyleMode(parsed.visualStyleMode)) out.visualStyleMode = parsed.visualStyleMode
    if (typeof parsed.customVisualStyle === 'string') out.customVisualStyle = parsed.customVisualStyle
    if (parsed.visualStylePresetId === null) {
      out.visualStylePresetId = null
    } else if (typeof parsed.visualStylePresetId === 'string') {
      const id = parsed.visualStylePresetId
      out.visualStylePresetId = getPresetById(id) ? id : null
      if (out.visualStylePresetId === null && parsed.visualStyleMode === 'preset') {
        out.visualStyleMode = 'custom'
      }
    }
    if (typeof parsed.visualStyleLongTextMode === 'boolean') {
      out.visualStyleLongTextMode = parsed.visualStyleLongTextMode
    }
    const maybeDuration = parseTargetDurationSec(parsed.targetDurationSec)
    if (maybeDuration != null) out.targetDurationSec = maybeDuration
    return out
  } catch {
    return {}
  }
}

function persist(snapshot: GlobalSettingsSnapshot) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot))
  } catch {
    /* ignore */
  }
}

type State = GlobalSettingsSnapshot & {
  init: () => void
  patch: (partial: Partial<GlobalSettingsSnapshot>) => void
  reset: () => void
}

export const useGlobalSettingsStore = create<State>((set) => ({
  ...DEFAULT,

  init: () => {
    const merged = { ...DEFAULT, ...loadPartial() }
    set((s) => ({ ...s, ...merged }))
  },

  patch: (partial) => {
    set((s) => {
      const next: GlobalSettingsSnapshot = {
        aspectRatio: partial.aspectRatio ?? s.aspectRatio,
        scriptType: partial.scriptType ?? s.scriptType,
        modelStrategy: partial.modelStrategy ?? s.modelStrategy,
        creationMode: partial.creationMode ?? s.creationMode,
        storyboardLayout: partial.storyboardLayout ?? s.storyboardLayout,
        visualStyleMode: partial.visualStyleMode ?? s.visualStyleMode,
        visualStylePresetId:
          partial.visualStylePresetId !== undefined ? partial.visualStylePresetId : s.visualStylePresetId,
        customVisualStyle: partial.customVisualStyle ?? s.customVisualStyle,
        visualStyleLongTextMode:
          partial.visualStyleLongTextMode !== undefined ? partial.visualStyleLongTextMode : s.visualStyleLongTextMode,
        targetDurationSec: partial.targetDurationSec ?? s.targetDurationSec,
      }
      persist(next)
      return next
    })
  },

  reset: () => {
    persist(DEFAULT)
    set((s) => ({ ...s, ...DEFAULT }))
  },
}))
