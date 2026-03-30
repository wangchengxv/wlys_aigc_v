import { getPresetById, resolveVisualStyleForProject, type GlobalStyleSlice } from '@/data/videoStylePresets'

const FALLBACK: GlobalStyleSlice = {
  visualStyleMode: 'custom',
  visualStylePresetId: null,
  customVisualStyle: '电影感写实',
  visualStyleLongTextMode: false,
}

/**
 * 与 aigc-site-react 的 `aigc-global-settings` localStorage 键一致，
 * 便于在 Vue 工作台复用同一份全局设定做视频风格解析。
 */
export function loadGlobalStyleSliceFromLocalStorage(): GlobalStyleSlice {
  try {
    const raw = localStorage.getItem('aigc-global-settings')
    if (!raw) return FALLBACK
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const out: GlobalStyleSlice = { ...FALLBACK }
    if (parsed.visualStyleMode === 'preset' || parsed.visualStyleMode === 'custom') {
      out.visualStyleMode = parsed.visualStyleMode
    }
    if (parsed.visualStylePresetId === null) {
      out.visualStylePresetId = null
    } else if (typeof parsed.visualStylePresetId === 'string') {
      const id = parsed.visualStylePresetId
      out.visualStylePresetId = getPresetById(id) ? id : null
    }
    if (typeof parsed.customVisualStyle === 'string') {
      out.customVisualStyle = parsed.customVisualStyle
    }
    if (typeof parsed.visualStyleLongTextMode === 'boolean') {
      out.visualStyleLongTextMode = parsed.visualStyleLongTextMode
    }
    return out
  } catch {
    return FALLBACK
  }
}

export function resolveVideoStyleFromGlobalSettings(): string {
  return resolveVisualStyleForProject(loadGlobalStyleSliceFromLocalStorage())
}
