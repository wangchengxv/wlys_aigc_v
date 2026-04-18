import { create } from 'zustand'
import { createStyleTemplate, getStyleTemplates, updateStyleTemplate } from '@/api'
import { FALLBACK_STYLE_TEMPLATES } from '@/data/videoStylePresets'
import type { StyleTemplate, StyleTemplateCreateRequest, StyleTemplateUpdateRequest } from '@/types'

type StyleTemplateState = {
  templates: StyleTemplate[]
  loading: boolean
  loaded: boolean
  saving: boolean
  error: string | null
  loadTemplates: (courseId?: string) => Promise<StyleTemplate[]>
  createTemplate: (payload: StyleTemplateCreateRequest) => Promise<StyleTemplate>
  updateTemplate: (templateId: string, payload: StyleTemplateUpdateRequest) => Promise<StyleTemplate>
}

let loadToken = 0

export const useStyleTemplateStore = create<StyleTemplateState>((set, get) => ({
  templates: FALLBACK_STYLE_TEMPLATES,
  loading: false,
  loaded: false,
  saving: false,
  error: null,

  loadTemplates: async (courseId) => {
    const token = ++loadToken
    set({ loading: true, error: null })
    try {
      const templates = await getStyleTemplates({ courseId })
      if (token === loadToken) {
        set({ templates: templates.length > 0 ? templates : FALLBACK_STYLE_TEMPLATES, loaded: true })
      }
      return templates
    } catch (error) {
      if (token === loadToken) {
        set({
          templates: get().templates.length > 0 ? get().templates : FALLBACK_STYLE_TEMPLATES,
          loaded: true,
          error: error instanceof Error ? error.message : '加载风格模板失败',
        })
      }
      return get().templates
    } finally {
      if (token === loadToken) {
        set({ loading: false })
      }
    }
  },

  createTemplate: async (payload) => {
    set({ saving: true, error: null })
    try {
      const created = await createStyleTemplate(payload)
      set((state) => ({ templates: [created, ...state.templates] }))
      return created
    } catch (error) {
      set({ error: error instanceof Error ? error.message : '创建风格模板失败' })
      throw error
    } finally {
      set({ saving: false })
    }
  },

  updateTemplate: async (templateId, payload) => {
    set({ saving: true, error: null })
    try {
      const updated = await updateStyleTemplate(templateId, payload)
      set((state) => ({
        templates: state.templates.map((item) => (item.templateId === templateId ? updated : item)),
      }))
      return updated
    } catch (error) {
      set({ error: error instanceof Error ? error.message : '更新风格模板失败' })
      throw error
    } finally {
      set({ saving: false })
    }
  },
}))
