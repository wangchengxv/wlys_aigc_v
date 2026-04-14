// Polyfill: Array.prototype.toReversed (ES2023) — required by @comfyorg/litegraph
if (!Array.prototype.toReversed) {
  Array.prototype.toReversed = function <T>(this: T[]): T[] {
    return this.slice().reverse()
  }
}

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { ToastProvider } from '@/context/ToastContext'
import { router } from '@/router'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'
import { useThemeStore } from '@/stores/themeStore'
import './styles/global.css'
import './styles/components.css'

useThemeStore.getState().initTheme()
useGlobalSettingsStore.getState().init()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ToastProvider>
      <RouterProvider router={router} />
    </ToastProvider>
  </StrictMode>,
)
