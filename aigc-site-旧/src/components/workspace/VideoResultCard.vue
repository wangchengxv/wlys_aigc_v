<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'

defineProps<{
  url: string
}>()

const emit = defineEmits<{
  (e: 'preview'): void
  (e: 'download'): void
  (e: 'copyLink'): void
  (e: 'open'): void
}>()
</script>

<template>
  <figure class="video-card panel">
    <video :src="url" controls preload="metadata" />
    <figcaption>
      <AppButton size="sm" @click="emit('preview')">预览视频</AppButton>
      <AppButton size="sm" @click="emit('download')">下载视频</AppButton>
      <AppButton size="sm" @click="emit('copyLink')">复制链接</AppButton>
      <AppButton size="sm" @click="emit('open')">新窗口播放</AppButton>
    </figcaption>
  </figure>
</template>

<style scoped>
.video-card {
  margin: 0;
  padding: var(--space-sm);
  background: var(--tint-primary-08);
}

video {
  width: 100%;
  display: block;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: #000;
}

figcaption {
  margin-top: var(--space-sm);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
</style>
