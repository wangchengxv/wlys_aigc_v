<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const toast = useToast()
</script>

<template>
  <Transition name="toast">
    <div v-if="toast.visible" class="toast" :class="`t-${toast.type}`" role="status" aria-live="polite">
      {{ toast.message }}
    </div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  z-index: var(--z-toast);
  padding: 10px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  color: var(--text-main);
  background: var(--toast-bg);
  box-shadow: var(--shadow-primary);
}

.t-success {
  border-color: color-mix(in srgb, var(--success) 60%, transparent);
}

.t-error {
  border-color: color-mix(in srgb, var(--danger) 70%, transparent);
}

.t-info {
  border-color: color-mix(in srgb, var(--primary) 60%, transparent);
}

.toast-enter-active,
.toast-leave-active {
  transition: all var(--duration-fast);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}
</style>
