<script setup lang="ts">
import type { GenerateMode } from '@/types'

defineProps<{
  mode: GenerateMode | 'all'
}>()

const emit = defineEmits<{
  (e: 'change', value: GenerateMode | 'all'): void
}>()
</script>

<template>
  <label class="filter">
    <span>模式筛选</span>
    <select :value="mode" @change="emit('change', ($event.target as HTMLSelectElement).value as GenerateMode | 'all')">
      <option value="all">全部模式</option>
      <option value="text">仅文本</option>
      <option value="image">仅图片</option>
      <option value="both">图文一起</option>
      <option value="video">仅视频</option>
    </select>
  </label>
</template>

<style scoped>
.filter {
  display: grid;
  gap: var(--space-xs);
}

span {
  color: var(--text-muted);
  font-size: 13px;
}

select {
  min-height: 44px;
  border: 1px solid var(--line);
  background: var(--field-bg);
  color: var(--text-main);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}
</style>
