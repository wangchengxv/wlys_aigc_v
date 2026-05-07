import { createRouter, createWebHistory } from 'vue-router'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/workspace', name: 'workspace', component: () => import('@/views/WorkspaceView.vue') },
    { path: '/script-projects', name: 'script-projects', component: () => import('@/views/ScriptProjectListView.vue') },
    { path: '/script-projects/new', name: 'script-project-create', component: () => import('@/views/ScriptProjectCreateView.vue') },
    { path: '/script-projects/:projectId', name: 'script-project-detail', component: () => import('@/views/ScriptProjectDetailView.vue') },
    { path: '/script-projects/:projectId/preview', name: 'script-project-preview', component: () => import('@/views/ScriptProjectPreviewView.vue') },
    { path: '/script-projects/:projectId/assets', name: 'script-project-assets', component: () => import('@/views/ScriptProjectAssetsView.vue') },
    { path: '/script-projects/:projectId/video', name: 'script-project-video', component: () => import('@/views/ScriptProjectVideoView.vue') },
    { path: '/history', name: 'history', component: () => import('@/views/HistoryView.vue') },
    { path: '/settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },
    { path: '/models', name: 'models', component: () => import('@/views/ModelConfigView.vue') },
    { path: '/router-console', name: 'router-console', component: () => import('@/views/RouterConsoleView.vue') },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
  ],
})
