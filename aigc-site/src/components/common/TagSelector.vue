<script setup lang="ts">
defineProps<{
  options: string[]
  selected: string
}>()

const emit = defineEmits<{
  (e: 'change', value: string): void
}>()
</script>

<template>
  <div class="tags">
    <button
      v-for="item in options"
      :key="item"
      class="tag"
      :class="{ active: item === selected }"
      type="button"
      @click="emit('change', item)"
    >
      {{ item }}
    </button>
  </div>
</template>

<style scoped>
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.tag {
  min-height: 36px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--tint-primary-08);
  color: var(--text-muted);
  padding: 6px 12px;
  cursor: pointer;
  transition: all var(--duration-fast);
}

.tag.active {
  color: var(--text-main);
  border-color: color-mix(in srgb, var(--primary) 45%, var(--line));
  background: var(--tint-primary-14);
}
</style>
