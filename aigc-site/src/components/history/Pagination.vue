<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'

defineProps<{
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  (e: 'change', page: number): void
}>()
</script>

<template>
  <div class="pager">
    <AppButton size="sm" :disabled="page <= 1" @click="emit('change', page - 1)">上一页</AppButton>
    <span>第 {{ page }} / {{ totalPages }} 页</span>
    <AppButton size="sm" :disabled="page >= totalPages" @click="emit('change', page + 1)">下一页</AppButton>
  </div>
</template>

<style scoped>
.pager {
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

span {
  color: var(--text-muted);
}
</style>
