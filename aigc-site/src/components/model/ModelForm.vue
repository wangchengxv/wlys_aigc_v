<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ConnectionConfig, ModelConfig, ModelConfigCreateRequest } from '@/types'

const props = defineProps<{
  visible: boolean
  editing?: ModelConfig | null
  connections: ConnectionConfig[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', data: ModelConfigCreateRequest): void
}>()

const form = ref<ModelConfigCreateRequest>({
  name: '',
  provider: '',
  modelName: '',
  connectionId: '',
  enabled: true,
  metadata: {},
})

watch(
  () => props.visible,
  (v) => {
    if (v && props.editing) {
      form.value = {
        name: props.editing.name,
        provider: props.editing.provider,
        modelName: props.editing.modelName,
        connectionId: props.editing.connectionId,
        enabled: props.editing.enabled,
        metadata: props.editing.metadata || {},
      }
    } else if (v) {
      form.value = { name: '', provider: '', modelName: '', connectionId: '', enabled: true, metadata: {} }
    }
  },
)

function handleSubmit() {
  if (!form.value.name || !form.value.provider || !form.value.modelName || !form.value.connectionId) return
  emit('submit', { ...form.value })
}
</script>

<template>
  <div v-if="visible" class="dialog-overlay" @click.self="emit('close')">
    <div class="dialog glass">
      <h3 class="dialog-title">{{ editing ? '编辑模型' : '新建模型' }}</h3>
      <form class="form" @submit.prevent="handleSubmit">
        <label class="input-wrap">
          <span class="label">名称</span>
          <input v-model="form.name" type="text" class="ctrl" placeholder="例如：GPT-4o" required />
        </label>
        <label class="input-wrap">
          <span class="label">提供商</span>
          <input v-model="form.provider" type="text" class="ctrl" placeholder="例如：OpenAI" required />
        </label>
        <label class="input-wrap">
          <span class="label">模型标识</span>
          <input v-model="form.modelName" type="text" class="ctrl" placeholder="例如：gpt-4o" required />
        </label>
        <label class="input-wrap">
          <span class="label">关联连接</span>
          <select v-model="form.connectionId" class="ctrl" required>
            <option value="" disabled>请选择连接</option>
            <option v-for="conn in connections" :key="conn.id" :value="conn.id">
              {{ conn.name }} ({{ conn.provider }})
            </option>
          </select>
        </label>
        <label class="input-wrap toggle-wrap">
          <span class="label">启用状态</span>
          <input v-model="form.enabled" type="checkbox" class="toggle" />
        </label>
        <div class="form-actions">
          <button type="button" class="btn-cancel" @click="emit('close')">取消</button>
          <button type="submit" class="btn-submit">{{ editing ? '保存' : '创建' }}</button>
        </div>
      </form>
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
  width: 100%;
  max-width: 480px;
}

.dialog-title {
  margin: 0 0 var(--space-lg);
  font-size: 1.125rem;
  font-weight: 600;
}

.form {
  display: grid;
  gap: var(--space-md);
}

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

select.ctrl {
  cursor: pointer;
}

.toggle-wrap {
  flex-direction: row;
  align-items: center;
  gap: var(--space-md);
}

.toggle {
  width: 44px;
  height: 24px;
  appearance: none;
  background: var(--line);
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background var(--duration-fast);
}

.toggle::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  transition: transform var(--duration-fast);
}

.toggle:checked {
  background: var(--primary);
}

.toggle:checked::after {
  transform: translateX(20px);
}

.form-actions {
  display: flex;
  gap: var(--space-md);
  justify-content: flex-end;
  margin-top: var(--space-md);
}

.btn-cancel,
.btn-submit {
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

.btn-submit {
  border: none;
  background: var(--primary);
  color: #fff;
}

.btn-submit:hover {
  opacity: 0.9;
}
</style>