<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'
import { resolveScriptFileUrl } from '@/services/api'
import type { StoryboardShot, VideoSegmentTask } from '@/types'

defineProps<{
  shot: StoryboardShot
  task?: VideoSegmentTask
  busy?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry', taskId: string): void
}>()
</script>

<template>
  <article class="segment panel glass">
    <div class="head">
      <div>
        <p class="eyebrow">Shot {{ shot.sequenceNo }}</p>
        <h3>{{ shot.title }}</h3>
      </div>
      <span class="status">{{ task?.status || shot.status }}</span>
    </div>

    <p class="muted">{{ shot.scriptText }}</p>
    <div class="meta">
      <span>动作：{{ shot.actionSummary }}</span>
      <span>运镜：{{ shot.cameraMovement }}</span>
    </div>

    <video
      v-if="task?.resultVideoFileId"
      class="video"
      :src="resolveScriptFileUrl(task.resultVideoFileId)"
      controls
      preload="metadata"
    />
    <div v-else class="video placeholder muted">视频结果将在生成完成后出现在这里</div>

    <div class="footer">
      <span class="muted">{{ task?.errorMessage || '任务就绪' }}</span>
      <AppButton
        v-if="task?.segmentTaskId && task.status === 'FAILED'"
        size="sm"
        :loading="busy"
        @click="emit('retry', task.segmentTaskId)"
      >
        重试片段
      </AppButton>
    </div>
  </article>
</template>

<style scoped>
.segment {
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-sm);
}

.head,
.footer {
  display: flex;
  justify-content: space-between;
  gap: var(--space-sm);
  align-items: center;
  flex-wrap: wrap;
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-muted);
}

h3,
p {
  margin: 0;
}

.status,
.meta span {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid var(--line);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.video {
  width: 100%;
  border-radius: var(--radius-md);
  background: #000;
  min-height: 240px;
}

.placeholder {
  display: grid;
  place-items: center;
  min-height: 240px;
  border-radius: var(--radius-md);
  border: 1px dashed var(--line);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}
</style>
