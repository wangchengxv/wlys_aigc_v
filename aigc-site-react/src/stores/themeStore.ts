import { create } from 'zustand'

export type ThemeId = 'light'

const STORAGE_KEY = 'aigc-theme'
const THEME_ORDER: ThemeId[] = ['light']

function isThemeId(value: string | null | undefined): value is ThemeId {
  return value === 'light'
}

function applyTheme(theme: ThemeId) {
  document.documentElement.dataset.theme = theme
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    /* ignore */
  }
}

type ThemeState = {
  theme: ThemeId
  initTheme: () => void
  setTheme: (next: ThemeId) => void
  toggle: () => void
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: 'light',

  initTheme: () => {
    applyTheme('light')
    set({ theme: 'light' })
  },

  setTheme: (next) => {
    const resolved = isThemeId(next) ? next : 'light'
    applyTheme(resolved)
    set({ theme: resolved })
  },

  toggle: () => {
    const i = THEME_ORDER.indexOf(get().theme)
    const next = THEME_ORDER[(i + 1) % THEME_ORDER.length]
    get().setTheme(next)
  },
}))
