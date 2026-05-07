import { create } from 'zustand'

export type ThemeId = 'light' | 'dark' | 'onelink'

const STORAGE_KEY = 'aigc-theme'
const THEME_ORDER: ThemeId[] = ['light', 'dark', 'onelink']

function isThemeId(value: string | null | undefined): value is ThemeId {
  return value === 'light' || value === 'dark' || value === 'onelink'
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
    let next: ThemeId = 'light'
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (isThemeId(raw)) next = raw
    } catch {
      /* ignore */
    }
    const dom = document.documentElement.dataset.theme
    if (isThemeId(dom)) next = dom
    applyTheme(next)
    set({ theme: next })
  },

  setTheme: (next) => {
    applyTheme(next)
    set({ theme: next })
  },

  toggle: () => {
    const i = THEME_ORDER.indexOf(get().theme)
    const next = THEME_ORDER[(i + 1) % THEME_ORDER.length]
    get().setTheme(next)
  },
}))
