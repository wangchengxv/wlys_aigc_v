<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import PageTransition from '@/components/common/PageTransition.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import ToastMessage from '@/components/common/ToastMessage.vue'

const route = useRoute()
const navOpen = ref(false)
const showBackTop = ref(false)

const titleMap: Record<string, string> = {
  home: '首页',
  workspace: '生成工作台',
  'script-projects': '剧本项目',
  'script-project-create': '新建剧本项目',
  'script-project-detail': '项目详情',
  'script-project-preview': '剧本预览',
  'script-project-assets': '资产与关键帧',
  'script-project-video': '视频生成',
  history: '历史记录',
  settings: '设置',
  'not-found': '页面不存在',
}

const eyebrowMap: Record<string, string> = {
  home: 'Overview',
  workspace: 'Create',
  'script-projects': 'Workflow',
  'script-project-create': 'Create',
  'script-project-detail': 'Project',
  'script-project-preview': 'Script',
  'script-project-assets': 'Assets',
  'script-project-video': 'Video',
  history: 'Library',
  settings: 'Preferences',
  'not-found': 'Error',
}

const pageTitle = computed(() => {
  const name = String(route.name ?? '')
  return titleMap[name] ?? 'AIGC 图文生成平台'
})

const pageEyebrow = computed(() => {
  const name = String(route.name ?? '')
  return eyebrowMap[name] ?? 'Studio'
})

function onScroll() {
  showBackTop.value = window.scrollY > 380
}

function backToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <div class="app-layout">
    <header class="top-nav panel glass">
      <div class="brand">
        <span class="dot" aria-hidden="true"></span>
        <span class="brand-text">
          <span class="brand-name">AIGC Studio</span>
          <span class="brand-tag">Image · Video · Text</span>
        </span>
      </div>

      <nav class="links" :class="{ open: navOpen }">
        <RouterLink to="/" @click="navOpen = false">首页</RouterLink>
        <RouterLink to="/workspace" @click="navOpen = false">生成工作台</RouterLink>
        <RouterLink to="/script-projects" @click="navOpen = false">剧本项目</RouterLink>
        <RouterLink to="/history" @click="navOpen = false">历史记录</RouterLink>
        <RouterLink to="/settings" @click="navOpen = false">设置</RouterLink>
      </nav>

      <ThemeToggle class="theme-slot" />

      <button class="hamburger" type="button" @click="navOpen = !navOpen">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </header>

    <main class="page-container">
      <header class="page-head">
        <p class="page-eyebrow">{{ pageEyebrow }}</p>
        <h1>{{ pageTitle }}</h1>
      </header>

      <RouterView v-slot="{ Component }">
        <PageTransition>
          <component :is="Component" />
        </PageTransition>
      </RouterView>
    </main>

    <AppButton v-if="showBackTop" class="back-top" size="sm" @click="backToTop">返回顶部</AppButton>
    <ToastMessage />
  </div>
</template>

<style scoped>
.app-layout {
  max-width: 1320px;
  margin: 0 auto;
  padding: var(--space-2xl) var(--space-xl);
}

.top-nav {
  margin-bottom: var(--space-xl);
  padding: 14px 22px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: var(--space-md);
  position: sticky;
  top: 16px;
  z-index: var(--z-nav);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.2;
}

.brand-tag {
  font-size: 0.6875rem;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.dot {
  flex-shrink: 0;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--primary), color-mix(in srgb, var(--secondary) 85%, var(--primary)));
  box-shadow: 0 0 20px color-mix(in srgb, var(--primary) 55%, transparent);
}

.hamburger {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--primary) 12%, transparent);
  padding: 8px;
}

.hamburger span {
  display: block;
  width: 100%;
  height: 2px;
  margin: 4px 0;
  background: var(--text-main);
}

.links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  justify-content: flex-end;
}

.theme-slot {
  justify-self: end;
}

.links :deep(a) {
  text-decoration: none;
  color: var(--text-muted);
  padding: 9px 14px;
  border-radius: var(--radius-sm);
  transition:
    color var(--duration-fast),
    background var(--duration-fast),
    box-shadow var(--duration-fast);
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.links :deep(a:hover) {
  color: var(--text-main);
}

.links :deep(.router-link-active) {
  color: var(--text-main);
  background: color-mix(in srgb, var(--primary) 14%, transparent);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary) 35%, transparent);
}

.page-head {
  margin-bottom: var(--space-lg);
}

.page-eyebrow {
  margin: 0 0 var(--space-xs);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.page-head h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.75rem, 3.6vw, 2.25rem);
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.2;
}

.page-container {
  min-height: calc(100vh - 180px);
}

.back-top {
  position: fixed;
  right: 20px;
  bottom: 20px;
}

@media (max-width: 900px) {
  .app-layout {
    padding: var(--space-md);
  }

  .top-nav {
    grid-template-columns: minmax(0, 1fr) auto auto;
    grid-template-rows: auto auto;
  }

  .brand {
    grid-column: 1;
    grid-row: 1;
  }

  .theme-slot {
    grid-column: 2;
    grid-row: 1;
  }

  .hamburger {
    display: inline-block;
    grid-column: 3;
    grid-row: 1;
  }

  .links {
    grid-column: 1 / -1;
    grid-row: 2;
    justify-content: stretch;
    display: none;
    flex-direction: column;
  }

  .links.open {
    display: flex;
  }
}
</style>
