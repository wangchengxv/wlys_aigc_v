<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import TagSelector from '@/components/common/TagSelector.vue'
import { useGenerationStore } from '@/stores/generation'
import { getImageModels, getVideoModels } from '@/services/api'
import type { GenerateMode } from '@/types'

const store = useGenerationStore()

const styles = ['科技风', '国潮风', '简约风', '可爱风', '商务风']
const sizes = ['512x512', '768x768', '1024x1024']
const DEFAULT_IMAGE_MODEL = 'doubao-seedream-5-0-260128'
const DEFAULT_VIDEO_MODEL = 'doubao-seedance-1-5-pro-251215'
const textLengthOptions = [
  { label: '短', value: 'short' },
  { label: '中', value: 'medium' },
  { label: '长', value: 'long' },
] as const

const form = reactive({
  prompt: '',
  style: '科技风',
  mode: 'both' as GenerateMode,
  imageSize: '1024x1024',
  textLength: 'medium' as 'short' | 'medium' | 'long',
  count: 1,
  imageModel: '',
  customImageModel: '',
  videoModel: DEFAULT_VIDEO_MODEL,
  customVideoModel: '',
})

const error = ref('')
const imageModelLoading = ref(false)
const videoModelLoading = ref(false)
const imageModelInputMode = ref<'preset' | 'custom'>('preset')
const videoModelInputMode = ref<'preset' | 'custom'>('preset')
const imageModelOptions = ref<string[]>([])
const videoModelOptions = ref<string[]>([])
const promptCount = computed(() => form.prompt.length)
const promptPercent = computed(() => Math.min(100, Math.round((promptCount.value / 500) * 100)))
const needsImageModel = computed(() => form.mode === 'image' || form.mode === 'both')
const needsVideoModel = computed(() => form.mode === 'video')
const needsTextLength = computed(() => form.mode === 'text' || form.mode === 'both')
const finalImageModel = computed(() => {
  if (imageModelInputMode.value === 'custom') {
    return form.customImageModel.trim()
  }
  return form.imageModel
})
const finalVideoModel = computed(() => {
  if (videoModelInputMode.value === 'custom') {
    return form.customVideoModel.trim()
  }
  return form.videoModel.trim()
})

const emit = defineEmits<{
  (e: 'generated'): void
}>()

async function onGenerate() {
  error.value = ''
  if (!form.prompt.trim()) {
    error.value = '请输入提示词后再生成'
    return
  }
  if (form.prompt.length > 500) {
    error.value = '提示词请控制在 500 字以内'
    return
  }
  if (/(暴恐|色情|违禁)/.test(form.prompt)) {
    error.value = '输入内容包含敏感词，请调整后重试'
    return
  }
  if (needsImageModel.value && !finalImageModel.value) {
    error.value = '请选择图片模型或填写自定义模型ID'
    return
  }
  if (needsVideoModel.value && !finalVideoModel.value) {
    error.value = '请输入视频模型ID'
    return
  }

  try {
    await store.generate({
      prompt: form.prompt.trim(),
      mode: form.mode,
      style: form.style,
      imageSize: form.imageSize,
      textLength: form.textLength,
      count: Math.max(1, Math.min(4, form.count)),
      imageModel: needsImageModel.value ? finalImageModel.value || undefined : undefined,
      videoModel: needsVideoModel.value ? finalVideoModel.value || undefined : undefined,
    })
    emit('generated')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '生成失败，请稍后重试'
  }
}

function clearAll() {
  form.prompt = ''
  form.customImageModel = ''
  form.customVideoModel = ''
  form.videoModel = DEFAULT_VIDEO_MODEL
  error.value = ''
  store.clearCurrent()
}

async function regenerate() {
  if (!store.currentTask) return
  form.prompt = store.currentTask.prompt
  const taskModel = store.currentTask.imageModel
  form.mode = store.currentTask.mode
  if (taskModel) {
    if (imageModelOptions.value.includes(taskModel)) {
      imageModelInputMode.value = 'preset'
      form.imageModel = taskModel
      form.customImageModel = ''
    } else {
      imageModelInputMode.value = 'custom'
      form.customImageModel = taskModel
    }
  }
  if (store.currentTask.videoModel) {
    if (videoModelOptions.value.includes(store.currentTask.videoModel)) {
      videoModelInputMode.value = 'preset'
      form.videoModel = store.currentTask.videoModel
      form.customVideoModel = ''
    } else {
      videoModelInputMode.value = 'custom'
      form.customVideoModel = store.currentTask.videoModel
    }
  }
  await onGenerate()
}

function quickGenerateVideo() {
  form.mode = 'video'
  onGenerate()
}

onMounted(async () => {
  imageModelLoading.value = true
  videoModelLoading.value = true
  const [imageRes, videoRes] = await Promise.allSettled([getImageModels(), getVideoModels()])

  if (imageRes.status === 'fulfilled') {
    imageModelOptions.value = Array.isArray(imageRes.value?.options) ? imageRes.value.options : []
    form.imageModel = imageRes.value?.defaultModel || imageModelOptions.value[0] || DEFAULT_IMAGE_MODEL
  } else {
    imageModelOptions.value = [DEFAULT_IMAGE_MODEL]
    form.imageModel = DEFAULT_IMAGE_MODEL
    error.value = '图片模型列表加载失败，已使用默认模型'
  }

  if (videoRes.status === 'fulfilled') {
    videoModelOptions.value = Array.isArray(videoRes.value?.options) ? videoRes.value.options : []
    form.videoModel = videoRes.value?.defaultModel || videoModelOptions.value[0] || DEFAULT_VIDEO_MODEL
  } else {
    videoModelOptions.value = [DEFAULT_VIDEO_MODEL]
    form.videoModel = DEFAULT_VIDEO_MODEL
    error.value = error.value ? `${error.value}；视频模型列表加载失败，已使用默认模型` : '视频模型列表加载失败，已使用默认模型'
  }

  imageModelLoading.value = false
  videoModelLoading.value = false
})
</script>

<template>
  <section class="prompt panel glass">
    <h2>生成工作台</h2>
    <AppInput
      v-model="form.prompt"
      label="提示词"
      as="textarea"
      placeholder="例如：春季服装上新活动，生成年轻化营销文案与海报配图..."
    />
    <div class="progress">
      <span class="muted">字数：{{ promptCount }}/500</span>
      <div class="bar">
        <span :style="{ width: `${promptPercent}%` }"></span>
      </div>
    </div>

    <div class="field">
      <p class="label">快捷风格</p>
      <TagSelector :options="styles" :selected="form.style" @change="form.style = $event" />
    </div>

    <div class="row">
      <label class="field">
        <span class="label">生成模式</span>
        <select v-model="form.mode">
          <option value="text">仅文本</option>
          <option value="image">仅图片</option>
          <option value="both">图文一起</option>
          <option value="video">仅视频</option>
        </select>
      </label>
      <label v-if="needsImageModel" class="field">
        <span class="label">图片尺寸</span>
        <select v-model="form.imageSize">
          <option v-for="item in sizes" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>
      <label v-else class="field">
        <span class="label">视频能力说明</span>
        <div class="tips-box muted">视频生成通常需要更长时间，建议等待 10~60 秒。</div>
      </label>
    </div>

    <div class="feature-actions">
      <AppButton variant="primary" :loading="store.loading && form.mode === 'video'" @click="quickGenerateVideo">
        {{ store.loading && form.mode === 'video' ? '视频生成中...' : 'AI视频生成' }}
      </AppButton>
      <p class="muted">一键切换到视频模式并直接生成</p>
    </div>

    <div class="row">
      <label v-if="needsTextLength" class="field">
        <span class="label">文本长度</span>
        <select v-model="form.textLength">
          <option v-for="item in textLengthOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <AppInput v-model="form.count" label="生成数量" type="number" :min="1" :max="4" />
    </div>

    <div v-if="needsImageModel" class="row">
      <label class="field">
        <span class="label">图片模型输入方式</span>
        <select v-model="imageModelInputMode">
          <option value="preset">从预置模型选择</option>
          <option value="custom">手动输入模型ID</option>
        </select>
      </label>
      <label v-if="imageModelInputMode === 'preset'" class="field">
        <span class="label">图片模型（默认豆包）</span>
        <select v-model="form.imageModel" :disabled="imageModelLoading">
          <option v-if="imageModelLoading" value="">加载中...</option>
          <option v-for="item in imageModelOptions" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>
      <AppInput
        v-else
        v-model="form.customImageModel"
        label="自定义模型ID"
        placeholder="请输入模型ID，例如：doubao-seedream-5-0-260128"
      />
    </div>

    <div v-if="needsVideoModel" class="row">
      <label class="field">
        <span class="label">视频模型输入方式</span>
        <select v-model="videoModelInputMode">
          <option value="preset">从预置模型选择</option>
          <option value="custom">手动输入模型ID</option>
        </select>
      </label>
      <label v-if="videoModelInputMode === 'preset'" class="field">
        <span class="label">视频模型（即梦）</span>
        <select v-model="form.videoModel" :disabled="videoModelLoading">
          <option v-if="videoModelLoading" value="">加载中...</option>
          <option v-for="item in videoModelOptions" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>
      <AppInput
        v-else
        v-model="form.customVideoModel"
        label="视频模型ID"
        placeholder="请输入视频模型ID，例如：doubao-seedance-1-5-pro-251215"
      />
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div class="actions">
      <AppButton variant="primary" :loading="store.loading" @click="onGenerate">
        {{ store.loading ? '生成中...' : '开始生成' }}
      </AppButton>
      <AppButton @click="regenerate">再来一版</AppButton>
      <AppButton @click="clearAll">清空</AppButton>
    </div>
  </section>
</template>

<style scoped>
.prompt {
  padding: var(--space-lg);
}

h2 {
  margin: 0 0 var(--space-lg);
}

.progress {
  margin-top: var(--space-sm);
  display: grid;
  gap: var(--space-xs);
}

.bar {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: var(--tint-primary-14);
  overflow: hidden;
}

.bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary), var(--secondary));
  transition: width var(--duration-fast);
}

.field {
  display: grid;
  gap: var(--space-sm);
}

.label {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.row {
  margin-top: var(--space-md);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

select {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--line);
  background: var(--field-bg);
  color: var(--text-main);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

select:focus {
  outline: none;
  border-color: var(--focus-border);
}

.actions {
  margin-top: var(--space-lg);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.feature-actions {
  margin-top: var(--space-md);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
}

.feature-actions p {
  margin: 0;
  font-size: 13px;
}

.tips-box {
  min-height: 44px;
  border: 1px solid var(--line);
  background: var(--tint-primary-08);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex;
  align-items: center;
}

.error {
  margin: var(--space-md) 0 0;
  color: var(--danger);
}

@media (max-width: 600px) {
  .row {
    grid-template-columns: 1fr;
  }

  .actions :deep(button) {
    width: 100%;
  }
}
</style>
