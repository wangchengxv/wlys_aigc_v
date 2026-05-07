<script setup lang="ts">
defineProps<{
  visible: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <div v-if="visible" class="dialog-overlay" @click.self="emit('cancel')">
    <div class="dialog glass">
      <h3 class="dialog-title">{{ title }}</h3>
      <p class="dialog-message">{{ message }}</p>
      <div class="dialog-actions">
        <button type="button" class="btn-cancel" @click="emit('cancel')">
          {{ cancelText || '取消' }}
        </button>
        <button type="button" class="btn-confirm" @click="emit('confirm')">
          {{ confirmText || '确认' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: var(--bg-main);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  min-width: 320px;
  max-width: 420px;
}

.dialog-title {
  margin: 0 0 var(--space-md);
  font-size: 1.125rem;
  font-weight: 600;
}

.dialog-message {
  margin: 0 0 var(--space-xl);
  color: var(--text-muted);
  line-height: 1.6;
}

.dialog-actions {
  display: flex;
  gap: var(--space-md);
  justify-content: flex-end;
}

.btn-cancel,
.btn-confirm {
  min-height: 40px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all var(--duration-fast);
}

.btn-cancel {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-muted);
}

.btn-cancel:hover {
  border-color: var(--text-muted);
  color: var(--text-main);
}

.btn-confirm {
  border: none;
  background: var(--primary);
  color: #fff;
}

.btn-confirm:hover {
  opacity: 0.9;
}
</style>
