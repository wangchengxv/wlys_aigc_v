import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'aigc-theme'

export type ThemeId = 'light' | 'dark' | 'onelink'

const THEME_ORDER: ThemeId[] = ['light', 'dark', 'onelink']

function isThemeId(value: string | null | undefined): value is ThemeId {
  return value === 'light' || value === 'dark' || value === 'onelink'
}

function readDomTheme(): ThemeId {
  const raw = document.documentElement.dataset.theme
  return isThemeId(raw) ? raw : 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<ThemeId>(readDomTheme())

  function apply() {
    document.documentElement.dataset.theme = theme.value
    try {
      localStorage.setItem(STORAGE_KEY, theme.value)
    } catch {
      /* ignore */
    }
  }

  /** 从 localStorage 同步并应用到 DOM（应用启动时调用一次） */
  function initTheme() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (isThemeId(raw)) {
        theme.value = raw
      }
    } catch {
      /* ignore */
    }
    apply()
  }

  function setTheme(next: ThemeId) {
    theme.value = next
    apply()
  }

  function toggle() {
    const i = THEME_ORDER.indexOf(theme.value)
    const next = THEME_ORDER[(i + 1) % THEME_ORDER.length]
    setTheme(next)
  }

  return { theme, setTheme, toggle, initTheme }
})
