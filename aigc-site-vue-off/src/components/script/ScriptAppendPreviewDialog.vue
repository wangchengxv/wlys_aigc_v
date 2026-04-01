<script setup lang="ts">
defineProps<{
  visible: boolean
  title?: string
  subtitle?: string
  appendText: string
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'cancel'): void
  (e: 'confirm'): void
}>()
</script>

<template>
  <div v-if="visible" class="dialog-overlay" role="dialog" aria-modal @click.self="emit('cancel')">
    <div class="dialog glass" style="max-width: 920px">
      <h3 class="dialog-title">{{ title || 'AI 续写剧本预览' }}</h3>
      <p v-if="subtitle" class="dialog-message">{{ subtitle }}</p>
      <div class="panel glass preview-body">
        {{ appendText || '（空）' }}
      </div>
      <div class="dialog-actions">
        <button type="button" class="btn-cancel" :disabled="loading" @click="emit('cancel')">取消</button>
        <button type="button" class="btn-confirm" :disabled="loading || !appendText.trim()" @click="emit('confirm')">追加并保存</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-body {
  max-height: 420px;
  overflow: auto;
  padding: var(--space-md);
  white-space: pre-wrap;
  line-height: 1.65;
}
</style>
