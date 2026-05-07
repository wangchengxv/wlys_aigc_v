<script setup lang="ts">
import { computed } from 'vue'
import type { PipelineStatus } from '@/types'

const props = defineProps<{
  pipeline: PipelineStatus | null
}>()

const percent = computed(() => {
  if (!props.pipeline || props.pipeline.totalCount === 0) return 0
  return Math.round(((props.pipeline.successCount + props.pipeline.failedCount) / props.pipeline.totalCount) * 100)
})
</script>

<template>
  <div class="pipeline panel glass">
    <div class="top">
      <div>
        <p class="label">流水线状态</p>
        <h3>{{ pipeline?.projectStatus || '未开始' }}</h3>
      </div>
      <strong>{{ percent }}%</strong>
    </div>
    <div class="track">
      <span :style="{ width: `${percent}%` }"></span>
    </div>
    <div class="stats muted">
      <span>总任务 {{ pipeline?.totalCount ?? 0 }}</span>
      <span>成功 {{ pipeline?.successCount ?? 0 }}</span>
      <span>失败 {{ pipeline?.failedCount ?? 0 }}</span>
      <span>运行中 {{ pipeline?.runningCount ?? 0 }}</span>
      <span>排队 {{ pipeline?.queuedCount ?? 0 }}</span>
    </div>
  </div>
</template>

<style scoped>
.pipeline {
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-sm);
}

.top,
.stats {
  display: flex;
  justify-content: space-between;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.label,
h3 {
  margin: 0;
}

.label {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-muted);
}

h3 {
  margin-top: 4px;
}

.track {
  height: 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  overflow: hidden;
}

.track span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--secondary));
}
</style>
