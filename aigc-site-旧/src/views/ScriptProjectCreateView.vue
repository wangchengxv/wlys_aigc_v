<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import { useToast } from '@/composables/useToast'
import { getModels } from '@/services/api'
import { useScriptProjectStore } from '@/stores/scriptProjects'
import type { ModelConfig } from '@/types'

const router = useRouter()
const toast = useToast()
const store = useScriptProjectStore()

const mode = ref<'text' | 'upload'>('text')
const uploadFile = ref<File | null>(null)

const form = reactive({
  name: '',
  sourceText: '',
  visualStyle: '电影感写实',
  aspectRatio: '16:9',
  targetDuration: 15,
  language: '中文',
  explicitTextModel: '',
  explicitImageModel: '',
  explicitVideoModel: '',
})

const error = ref('')
const warnings = ref<string[]>([])
const modelLoading = ref(false)
const modelOptions = ref<ModelConfig[]>([])
const textModelInputMode = ref<'preset' | 'custom'>('preset')
const imageModelInputMode = ref<'preset' | 'custom'>('preset')
const videoModelInputMode = ref<'preset' | 'custom'>('preset')

const textModelOptions = computed(() =>
  modelOptions.value
    .filter((item) => hasCapability(item, 'text'))
    .map((item) => item.modelName),
)
const imageModelOptions = computed(() =>
  modelOptions.value
    .filter((item) => hasCapability(item, 'image'))
    .map((item) => item.modelName),
)
const videoModelOptions = computed(() =>
  modelOptions.value
    .filter((item) => hasCapability(item, 'video'))
    .map((item) => item.modelName),
)

function hasCapability(model: ModelConfig, capability: 'text' | 'image' | 'video') {
  const caps = Array.isArray(model.metadata?.capabilities) ? (model.metadata.capabilities as string[]) : []
  return caps.map((item) => String(item).toLowerCase()).includes(capability)
}

function normalizeExplicitValue(value: string, mode: 'preset' | 'custom') {
  if (mode === 'custom') return value.trim()
  return value
}

onMounted(async () => {
  modelLoading.value = true
  try {
    const allModels = await getModels()
    modelOptions.value = allModels.filter((item) => item.enabled)
    if (!form.explicitTextModel && textModelOptions.value.length > 0) {
      form.explicitTextModel = textModelOptions.value[0]
    }
    if (!form.explicitImageModel && imageModelOptions.value.length > 0) {
      form.explicitImageModel = imageModelOptions.value[0]
    }
    if (!form.explicitVideoModel && videoModelOptions.value.length > 0) {
      form.explicitVideoModel = videoModelOptions.value[0]
    }
  } catch (e) {
    warnings.value.push(e instanceof Error ? `模型列表加载失败：${e.message}` : '模型列表加载失败')
  } finally {
    modelLoading.value = false
  }
})

async function submit() {
  error.value = ''
  warnings.value = []
  form.explicitTextModel = normalizeExplicitValue(form.explicitTextModel, textModelInputMode.value)
  form.explicitImageModel = normalizeExplicitValue(form.explicitImageModel, imageModelInputMode.value)
  form.explicitVideoModel = normalizeExplicitValue(form.explicitVideoModel, videoModelInputMode.value)
  if (textModelInputMode.value === 'custom' && form.explicitTextModel && !textModelOptions.value.includes(form.explicitTextModel)) {
    warnings.value.push('文本模型不在已配置模型中，运行时可能走系统 fallback。')
  }
  if (imageModelInputMode.value === 'custom' && form.explicitImageModel && !imageModelOptions.value.includes(form.explicitImageModel)) {
    warnings.value.push('图片模型不在已配置模型中，运行时可能走系统 fallback。')
  }
  if (videoModelInputMode.value === 'custom' && form.explicitVideoModel && !videoModelOptions.value.includes(form.explicitVideoModel)) {
    warnings.value.push('视频模型不在已配置模型中，运行时可能走系统 fallback。')
  }
  try {
    let result
    if (mode.value === 'text') {
      if (!form.sourceText.trim()) {
        error.value = '请输入剧本文本'
        return
      }
      result = await store.createFromText({ ...form, sourceText: form.sourceText.trim() })
    } else {
      if (!uploadFile.value) {
        error.value = '请选择要上传的剧本文件'
        return
      }
      result = await store.createFromUpload({
        ...form,
        file: uploadFile.value,
      })
    }
    toast.showToast('项目创建成功', 'success')
    router.push(`/script-projects/${result.project.projectId}/preview`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建项目失败'
  }
}

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  uploadFile.value = target.files?.[0] || null
}
</script>

<template>
  <section class="page">
    <div class="mode-switch panel glass">
      <button :class="{ active: mode === 'text' }" @click="mode = 'text'">粘贴文本</button>
      <button :class="{ active: mode === 'upload' }" @click="mode = 'upload'">上传文件</button>
    </div>

    <form class="form panel glass" @submit.prevent="submit">
      <div class="grid">
        <AppInput v-model="form.name" label="项目名称" placeholder="例如：都市悬疑短片" />
        <AppInput v-model="form.visualStyle" label="视觉风格" placeholder="例如：电影感写实 / 国风 / 赛博朋克" />
        <AppInput v-model="form.aspectRatio" label="视频比例" placeholder="16:9 / 9:16 / 1:1" />
        <AppInput v-model="form.targetDuration" label="目标时长（秒）" type="number" :min="1" :max="600" />
        <AppInput v-model="form.language" label="输出语言" placeholder="中文" />
        <label class="model-field">
          <span class="label">文本模型（可选）</span>
          <div class="model-row">
            <select v-model="textModelInputMode">
              <option value="preset">从已配置模型选择</option>
              <option value="custom">手动输入</option>
            </select>
            <select v-if="textModelInputMode === 'preset'" v-model="form.explicitTextModel" :disabled="modelLoading">
              <option value="">未填则走系统路由</option>
              <option v-for="item in textModelOptions" :key="item" :value="item">{{ item }}</option>
            </select>
            <AppInput v-else v-model="form.explicitTextModel" placeholder="请输入模型标识（modelName）" />
          </div>
        </label>
        <label class="model-field">
          <span class="label">图片模型（可选）</span>
          <div class="model-row">
            <select v-model="imageModelInputMode">
              <option value="preset">从已配置模型选择</option>
              <option value="custom">手动输入</option>
            </select>
            <select v-if="imageModelInputMode === 'preset'" v-model="form.explicitImageModel" :disabled="modelLoading">
              <option value="">未填则走系统路由</option>
              <option v-for="item in imageModelOptions" :key="item" :value="item">{{ item }}</option>
            </select>
            <AppInput v-else v-model="form.explicitImageModel" placeholder="请输入模型标识（modelName）" />
          </div>
        </label>
        <label class="model-field">
          <span class="label">视频模型（可选）</span>
          <div class="model-row">
            <select v-model="videoModelInputMode">
              <option value="preset">从已配置模型选择</option>
              <option value="custom">手动输入</option>
            </select>
            <select v-if="videoModelInputMode === 'preset'" v-model="form.explicitVideoModel" :disabled="modelLoading">
              <option value="">未填则走系统路由</option>
              <option v-for="item in videoModelOptions" :key="item" :value="item">{{ item }}</option>
            </select>
            <AppInput v-else v-model="form.explicitVideoModel" placeholder="请输入模型标识（modelName）" />
          </div>
        </label>
      </div>

      <AppInput
        v-if="mode === 'text'"
        v-model="form.sourceText"
        label="剧本文本"
        as="textarea"
        :rows="14"
        placeholder="请直接粘贴剧本文本，支持分段、对白和场景说明。"
      />

      <label v-else class="upload-box">
        <span class="label">上传剧本文件</span>
        <input accept=".txt,.md,.docx" type="file" @change="onFileChange" />
        <span class="muted">{{ uploadFile?.name || '支持 .txt / .md / .docx' }}</span>
      </label>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-for="item in warnings" :key="item" class="warning">{{ item }}</p>

      <div class="actions">
        <AppButton type="submit" variant="primary" :loading="store.createLoading">创建并进入剧本预览</AppButton>
        <RouterLink class="back-link" to="/script-projects">返回列表</RouterLink>
      </div>
    </form>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: var(--space-lg);
}

.mode-switch,
.form {
  padding: var(--space-xl);
}

.mode-switch {
  display: flex;
  gap: var(--space-sm);
}

.mode-switch button {
  min-height: 42px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  color: var(--text-main);
}

.mode-switch button.active {
  background: color-mix(in srgb, var(--primary) 18%, transparent);
}

.form {
  display: grid;
  gap: var(--space-lg);
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.upload-box {
  display: grid;
  gap: var(--space-sm);
}

.model-field {
  display: grid;
  gap: var(--space-sm);
}

.model-row {
  display: grid;
  gap: var(--space-sm);
}

.label {
  font-size: 13px;
  color: var(--text-muted);
}

input[type='file'] {
  width: 100%;
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px dashed var(--line);
  background: var(--field-bg);
  color: var(--text-main);
}

.actions {
  display: flex;
  gap: var(--space-sm);
  align-items: center;
  flex-wrap: wrap;
}

.back-link {
  text-decoration: none;
  color: var(--text-muted);
}

.error {
  margin: 0;
  color: var(--danger);
}

.warning {
  margin: 0;
  color: var(--warning, #f59e0b);
}

@media (max-width: 760px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
