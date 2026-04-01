<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: string | number
    label?: string
    as?: 'input' | 'textarea'
    placeholder?: string
    type?: string
    rows?: number
    min?: number
    max?: number
  }>(),
  {
    as: 'input',
    placeholder: '',
    type: 'text',
    rows: 5,
    min: undefined,
    max: undefined,
    label: '',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
}>()

function onUpdate(event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  if (target instanceof HTMLInputElement && target.type === 'number') {
    emit('update:modelValue', Number(target.value))
    return
  }
  emit('update:modelValue', target.value)
}
</script>

<template>
  <label class="input-wrap">
    <span v-if="label" class="label">{{ label }}</span>
    <textarea
      v-if="as === 'textarea'"
      class="ctrl"
      :rows="rows"
      :placeholder="placeholder"
      :value="String(modelValue)"
      @input="onUpdate"
    />
    <input
      v-else
      class="ctrl"
      :type="type"
      :placeholder="placeholder"
      :value="String(modelValue)"
      :min="min"
      :max="max"
      @input="onUpdate"
    />
  </label>
</template>

<style scoped>
.input-wrap {
  display: grid;
  gap: var(--space-sm);
}

.label {
  font-size: 13px;
  color: var(--text-muted);
}

.ctrl {
  width: 100%;
  border: 1px solid var(--line);
  background: var(--field-bg);
  color: var(--text-main);
  border-radius: var(--radius-md);
  padding: 11px 12px;
  transition: border-color var(--duration-fast);
}

.ctrl:focus {
  outline: none;
  border-color: var(--focus-border);
}

textarea.ctrl {
  min-height: 124px;
  resize: vertical;
}
</style>
