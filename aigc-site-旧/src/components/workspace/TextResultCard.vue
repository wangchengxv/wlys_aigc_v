<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'

defineProps<{
  text: string
  favorite: boolean
}>()

const emit = defineEmits<{
  (e: 'copy'): void
  (e: 'favorite'): void
}>()
</script>

<template>
  <article class="text-card panel">
    <p>{{ text }}</p>
    <div class="actions">
      <AppButton size="sm" @click="emit('copy')">复制</AppButton>
      <AppButton size="sm" @click="emit('favorite')">{{ favorite ? '已收藏' : '收藏' }}</AppButton>
    </div>
  </article>
</template>

<style scoped>
.text-card {
  padding: var(--space-lg);
  background: var(--tint-primary-08);
  transition:
    transform var(--duration-fast),
    border-color var(--duration-fast);
}

.text-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--primary) 36%, var(--line));
}

p {
  margin: 0;
  line-height: 1.8;
}

.actions {
  margin-top: var(--space-md);
  display: flex;
  gap: var(--space-sm);
}
</style>
