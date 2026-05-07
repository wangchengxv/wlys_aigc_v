<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'
import type { GenerateResponse } from '@/types'

defineProps<{
  task: GenerateResponse
}>()

const emit = defineEmits<{
  (e: 'open'): void
  (e: 'remove'): void
}>()

function statusText(status: GenerateResponse['status']) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'PROCESSING') return '处理中'
  return '失败'
}

function modeText(mode: GenerateResponse['mode']) {
  if (mode === 'text') return '仅文本'
  if (mode === 'image') return '仅图片'
  if (mode === 'video') return '仅视频'
  return '图文一起'
}
</script>

<template>
  <article class="history-card panel glass">
    <div class="meta">
      <span class="status" :class="`s-${task.status.toLowerCase()}`">{{ statusText(task.status) }}</span>
      <span class="muted">{{ new Date(task.createdAt).toLocaleString() }}</span>
    </div>
    <p class="title">{{ task.prompt }}</p>
    <p class="sub muted">
      {{ modeText(task.mode) }} | {{ task.style }} | 文{{ task.textResults.length }} 图{{ task.imageResults.length }} 视{{ task.videoResults.length }} | {{ task.latencyMs }}ms
    </p>
    <div class="actions">
      <AppButton size="sm" @click="emit('open')">查看并重生成</AppButton>
      <AppButton size="sm" variant="danger" @click="emit('remove')">删除记录</AppButton>
    </div>
  </article>
</template>

<style scoped>
.history-card {
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-sm);
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.status {
  min-height: 24px;
  border-radius: 999px;
  padding: 2px 10px;
  border: 1px solid var(--line);
  font-size: 12px;
}

.s-success {
  color: var(--success);
  border-color: color-mix(in srgb, var(--success) 60%, transparent);
}

.s-processing {
  color: var(--primary);
  border-color: color-mix(in srgb, var(--primary) 60%, transparent);
}

.s-fail {
  color: var(--danger);
  border-color: color-mix(in srgb, var(--danger) 60%, transparent);
}

.title {
  margin: 0;
  font-weight: 700;
  line-height: 1.7;
}

.sub {
  margin: 0;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}
</style>
