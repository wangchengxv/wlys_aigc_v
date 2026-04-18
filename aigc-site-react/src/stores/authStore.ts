import { create } from 'zustand'
import { getCurrentUser, getStoredAccessToken, login, logout } from '@/api'
import type { CurrentUser } from '@/types'

type AuthState = {
  user: CurrentUser | null
  initialized: boolean
  loading: boolean
  loggingIn: boolean
  error: string | null
  init: () => Promise<void>
  refresh: () => Promise<void>
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  initialized: false,
  loading: false,
  loggingIn: false,
  error: null,

  init: async () => {
    if (get().initialized || get().loading) return
    if (!getStoredAccessToken() && import.meta.env.VITE_API_BASE_URL) {
      set({ user: null, initialized: true, loading: false, error: null })
      return
    }
    set({ loading: true, error: null })
    try {
      const user = await getCurrentUser()
      set({ user, initialized: true })
    } catch (error) {
      if (getStoredAccessToken()) {
        await logout()
      }
      set({ user: null, initialized: true, error: error instanceof Error ? error.message : '获取登录状态失败' })
    } finally {
      set({ loading: false })
    }
  },

  refresh: async () => {
    set({ loading: true, error: null })
    try {
      const user = await getCurrentUser()
      set({ user, initialized: true })
    } catch (error) {
      set({ error: error instanceof Error ? error.message : '刷新当前用户失败' })
      throw error
    } finally {
      set({ loading: false })
    }
  },

  signIn: async (username, password) => {
    set({ loggingIn: true, error: null })
    try {
      const payload = await login({ username, password })
      set({ user: payload.user, initialized: true })
    } catch (error) {
      set({ error: error instanceof Error ? error.message : '登录失败' })
      throw error
    } finally {
      set({ loggingIn: false })
    }
  },

  signOut: async () => {
    set({ loading: true, error: null })
    try {
      await logout()
      set({ user: null, initialized: true })
    } finally {
      set({ loading: false })
    }
  },
}))
