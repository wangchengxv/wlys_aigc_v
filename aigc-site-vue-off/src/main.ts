import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { useGlobalSettingsStore } from '@/stores/globalSettings'
import { useThemeStore } from '@/stores/theme'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

useThemeStore().initTheme()
useGlobalSettingsStore().init()

app.mount('#app')
