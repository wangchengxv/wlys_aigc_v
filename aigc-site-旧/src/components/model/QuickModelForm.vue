<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getPresetModels, quickCreateConnection } from '@/services/api'
import { useToast } from '@/composables/useToast'
import type { PresetModelDto, QuickConnectionRequest } from '@/types'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success'): void
}>()

const { showToast } = useToast()

const loading = ref(false)
const presetData = ref<{ models: PresetModelDto[]; providers: string[] }>({ models: [], providers: [] })
const selectedProvider = ref('')
const selectedModel = ref('')
const apiKey = ref('')
const unknownModel = ref(false)

const modelOptions = computed(() => {
  if (!selectedProvider.value) return []
  return presetData.value.models.filter((m) => m.provider === selectedProvider.value)
})

watch(
  () => props.visible,
  async (v) => {
    if (v) {
      selectedProvider.value = ''
      selectedModel.value = ''
      apiKey.value = ''
      unknownModel.value = false
      try {
        presetData.value = await getPresetModels()
      } catch (e) {
        showToast((e as Error)?.message || '加载预置模型失败', 'error')
      }
    }
  },
)

watch(selectedModel, (val) => {
  if (val && !presetData.value.models.some((m) => m.modelName === val && m.provider === selectedProvider.value)) {
    unknownModel.value = true
  } else {
    unknownModel.value = false
  }
})

async function handleSubmit() {
  if (!selectedProvider.value || !selectedModel.value || !apiKey.value) return
  loading.value = true
  try {
    const req: QuickConnectionRequest = {
      provider: selectedProvider.value,
      modelName: selectedModel.value,
      apiKey: apiKey.value,
      enabled: true,
    }
    await quickCreateConnection(req)
    showToast('连接与模型已创建')
    emit('success')
    emit('close')
  } catch (e) {
    showToast((e as Error)?.message || '创建失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-if="visible" class="dialog-overlay" @click.self="emit('close')">
    <div class="dialog glass">
      <h3 class="dialog-title">快捷配置</h3>
      <p class="dialog-desc">选择模型并输入 API Key，即可快速完成配置</p>
      <form class="form" @submit.prevent="handleSubmit">
        <label class="input-wrap">
          <span class="label">提供商</span>
          <select v-model="selectedProvider" class="ctrl" required>
            <option value="" disabled>请选择提供商</option>
            <option v-for="p in presetData.providers" :key="p" :value="p">{{ p }}</option>
          </select>
        </label>
        <label class="input-wrap">
          <span class="label">模型</span>
          <select v-model="selectedModel" class="ctrl" :disabled="!selectedProvider" required>
            <option value="" disabled>{{ selectedProvider ? '请选择模型' : '请先选择提供商' }}</option>
            <option v-for="m in modelOptions" :key="m.modelName" :value="m.modelName">
              {{ m.displayName }} ({{ m.modelName }})
            </option>
          </select>
        </label>
        <div v-if="unknownModel" class="unknown-hint">
          该模型不在预置库中，请切换到<button type="button" class="link-btn" @click="emit('close')">高级模式</button>手动配置
        </div>
        <label class="input-wrap">
          <span class="label">API Key</span>
          <input v-model="apiKey" type="password" class="ctrl" placeholder="请输入 API Key" required />
        </label>
        <div class="form-actions">
          <button type="button" class="btn-cancel" @click="emit('close')">取消</button>
          <button type="submit" class="btn-submit" :disabled="loading || unknownModel || !selectedProvider || !selectedModel || !apiKey">
            {{ loading ? '创建中...' : '创建' }}
          </button>
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
  margin: 0 0 var(--space-xs);
  font-size: 1.125rem;
  font-weight: 600;
}

.dialog-desc {
  margin: 0 0 var(--space-lg);
  font-size: 0.875rem;
  color: var(--text-muted);
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

.ctrl:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

select.ctrl {
  cursor: pointer;
}

.unknown-hint {
  font-size: 0.8125rem;
  color: var(--text-muted);
  background: var(--tint-primary-14);
  border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.link-btn {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  font-size: inherit;
  padding: 0;
  text-decoration: underline;
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

.btn-submit:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
