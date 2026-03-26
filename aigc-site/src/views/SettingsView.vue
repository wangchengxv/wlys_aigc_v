<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { getApiBaseUrl, healthCheck } from '@/services/api'

const router = useRouter()
const themeStore = useThemeStore()

const status = ref<'loading' | 'ok' | 'fail'>('loading')
const mode = ref('unknown')
const envMode = import.meta.env.MODE
const apiBaseUrl = getApiBaseUrl()

onMounted(async () => {
  try {
    const res = await healthCheck()
    status.value = res.ok ? 'ok' : 'fail'
    mode.value = res.mode
  } catch {
    status.value = 'fail'
  }
})
</script>

<template>
  <section class="panel glass settings-page">
    <h2>设置</h2>
    <div class="setting-item theme-block">
      <p>外观</p>
      <p class="hint muted">浅色 / 深色 / OneLink（青靛科技风，参考 onelinkai.cloud）</p>
      <div class="theme-pills" role="group" aria-label="主题选择">
        <button
          type="button"
          class="pill"
          :class="{ active: themeStore.theme === 'light' }"
          @click="themeStore.setTheme('light')"
        >
          浅色
        </button>
        <button
          type="button"
          class="pill"
          :class="{ active: themeStore.theme === 'dark' }"
          @click="themeStore.setTheme('dark')"
        >
          深色
        </button>
        <button
          type="button"
          class="pill"
          :class="{ active: themeStore.theme === 'onelink' }"
          @click="themeStore.setTheme('onelink')"
        >
          OneLink
        </button>
      </div>
    </div>
    <div class="setting-item">
      <p>模型服务状态</p>
      <strong :class="status" class="health">
        <span class="dot" :class="status"></span>
        {{
          status === 'loading'
            ? '检测中...'
            : status === 'ok'
              ? `正常（${mode.toUpperCase()}）`
              : '异常，请检查后端服务'
        }}
      </strong>
    </div>
    <div class="setting-item">
      <p>系统信息</p>
      <ul>
        <li>运行模式：{{ envMode }}</li>
        <li>接口地址：{{ apiBaseUrl }}</li>
        <li>前端版本：v1.0</li>
      </ul>
    </div>
    <div class="setting-item">
      <p>模型配置管理</p>
      <button type="button" class="pill" @click="router.push('/models')">
        前往配置
      </button>
    </div>
    <div class="setting-item">
      <p>使用说明</p>
      <ul>
        <li>1. 输入主题提示词，选择生成模式与风格。</li>
        <li>2. 图文模式可选图片模型；视频模式可选即梦视频模型。</li>
        <li>3. 点击开始生成，视频任务可能需要 10~60 秒返回。</li>
        <li>4. 支持复制文案、下载图片/视频、复制视频链接、历史回看。</li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.settings-page {
  padding: var(--space-xl);
  display: grid;
  gap: var(--space-md);
}

h2,
p {
  margin: 0;
}

.setting-item {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--tint-primary-08);
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-sm);
}

ul {
  margin: 0;
  padding-left: 20px;
  color: var(--text-muted);
  line-height: 1.8;
}

.health {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.ok {
  color: var(--success);
}

.ok.dot {
  background: var(--success);
  box-shadow: 0 0 10px var(--success);
}

.fail {
  color: var(--danger);
}

.fail.dot {
  background: var(--danger);
  box-shadow: 0 0 10px var(--danger);
}

.theme-block .hint {
  margin: 0;
  font-size: 0.8125rem;
  line-height: 1.5;
}

.theme-pills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.pill {
  min-height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--bg-main) 40%, transparent);
  color: var(--text-muted);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition:
    color var(--duration-fast),
    background var(--duration-fast),
    border-color var(--duration-fast),
    box-shadow var(--duration-fast);
}

.pill:hover {
  color: var(--text-main);
  border-color: color-mix(in srgb, var(--primary) 35%, var(--line));
}

.pill.active {
  color: var(--text-main);
  background: var(--tint-primary-14);
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary) 28%, transparent);
}
</style>
