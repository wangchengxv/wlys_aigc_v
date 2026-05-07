<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'
import { resolveScriptFileUrl } from '@/services/api'
import type { KeyframeRecord } from '@/types'

const props = defineProps<{
  item: KeyframeRecord
  busy?: boolean
}>()

const emit = defineEmits<{
  (e: 'confirm', keyframeId: string): void
  (e: 'regenerate', keyframeId: string): void
}>()
</script>

<template>
  <article class="keyframe panel glass">
    <img v-if="item.imageFileId" class="image" :src="resolveScriptFileUrl(item.imageFileId)" :alt="item.promptText" />
    <div v-else class="image placeholder">暂无图像</div>
    <div class="content">
      <p class="muted clamp">{{ item.promptText }}</p>
      <div class="actions">
        <AppButton size="sm" :variant="item.selected ? 'primary' : 'ghost'" :loading="busy" @click="emit('confirm', item.keyframeId)">
          {{ item.selected ? '已确认' : '确认选中' }}
        </AppButton>
        <AppButton size="sm" :loading="busy" @click="emit('regenerate', item.keyframeId)">重新生成</AppButton>
      </div>
    </div>
  </article>
</template>

<style scoped>
.keyframe {
  overflow: hidden;
}

.image {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
  background: color-mix(in srgb, var(--primary) 10%, transparent);
}

.placeholder {
  display: grid;
  place-items: center;
}

.content {
  padding: var(--space-md);
  display: grid;
  gap: var(--space-sm);
}

.clamp {
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}
</style>
