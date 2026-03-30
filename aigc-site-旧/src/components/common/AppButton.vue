<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'ghost' | 'danger'
    size?: 'sm' | 'md'
    loading?: boolean
    disabled?: boolean
    block?: boolean
    type?: 'button' | 'submit' | 'reset'
  }>(),
  {
    variant: 'ghost',
    size: 'md',
    loading: false,
    disabled: false,
    block: false,
    type: 'button',
  },
)
</script>

<template>
  <button
    class="app-btn"
    :class="[`v-${props.variant}`, `s-${props.size}`, { block: props.block }]"
    :disabled="props.disabled || props.loading"
    :type="props.type"
  >
    <span v-if="props.loading" class="spinner" aria-hidden="true"></span>
    <slot />
  </button>
</template>

<style scoped>
.app-btn {
  min-height: 44px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  padding: 10px 14px;
  cursor: pointer;
  transition:
    transform var(--duration-fast),
    box-shadow var(--duration-fast),
    border-color var(--duration-fast),
    opacity var(--duration-fast);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.app-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.app-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.app-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.v-primary {
  color: var(--btn-primary-fg, #141210);
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--primary) 92%, #fff),
    color-mix(in srgb, var(--secondary) 75%, var(--primary))
  );
  box-shadow: 0 12px 28px color-mix(in srgb, var(--primary) 28%, transparent);
}

.v-primary:hover:not(:disabled) {
  box-shadow: 0 16px 36px color-mix(in srgb, var(--primary) 38%, transparent);
}

.v-ghost {
  color: var(--text-main);
  border-color: var(--line);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.v-danger {
  color: #fff;
  border-color: rgba(255, 111, 159, 0.4);
  background: linear-gradient(135deg, rgba(255, 111, 159, 0.9), rgba(226, 72, 136, 0.92));
}

.s-sm {
  min-height: 36px;
  padding: 7px 12px;
  border-radius: var(--radius-sm);
}

.block {
  width: 100%;
}

.spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid color-mix(in srgb, currentColor 32%, transparent);
  border-top-color: currentColor;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
