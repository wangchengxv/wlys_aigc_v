<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'

defineProps<{
  url: string
}>()

const emit = defineEmits<{
  (e: 'preview'): void
  (e: 'download'): void
}>()
</script>

<template>
  <figure class="img-card panel">
    <button class="preview" type="button" @click="emit('preview')">
      <img :src="url" alt="生成图片" />
    </button>
    <figcaption>
      <AppButton size="sm" @click="emit('download')">下载</AppButton>
    </figcaption>
  </figure>
</template>

<style scoped>
.img-card {
  margin: 0;
  padding: var(--space-sm);
  background: var(--tint-primary-08);
}

.preview {
  display: block;
  width: 100%;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0;
  background: none;
  cursor: zoom-in;
  overflow: hidden;
}

img {
  width: 100%;
  display: block;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  transition: transform var(--duration-normal);
}

.preview:hover img {
  transform: scale(1.03);
}

figcaption {
  margin-top: var(--space-sm);
}
</style>
