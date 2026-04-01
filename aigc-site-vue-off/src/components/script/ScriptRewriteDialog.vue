<script setup lang="ts">
import AppInput from '@/components/common/AppInput.vue'
import ScriptRewriteDiffPanel from '@/components/script/ScriptRewriteDiffPanel.vue'
import type { RewriteDiffMode } from '@/components/script/ScriptRewriteDiffPanel.vue'

defineProps<{
  visible: boolean
  instruction: string
  targetStyle: string
  maxOutputChars: string
  originalText: string
  previewText: string
  diffMode: RewriteDiffMode
  loading?: boolean
  applying?: boolean
  previewSubtitle?: string
}>()

const emit = defineEmits<{
  (e: 'change:instruction', v: string): void
  (e: 'change:targetStyle', v: string): void
  (e: 'change:maxOutputChars', v: string): void
  (e: 'change:diffMode', v: RewriteDiffMode): void
  (e: 'cancel'): void
  (e: 'preview'): void
  (e: 'apply'): void
}>()
</script>

<template>
  <div v-if="visible" class="dialog-overlay" role="dialog" aria-modal @click.self="emit('cancel')">
    <div class="dialog glass" style="max-width: 980px">
      <h3 class="dialog-title">AI 剧本改写</h3>
      <div class="grid">
        <AppInput
          label="改写要求"
          as="textarea"
          :rows="4"
          :model-value="instruction"
          placeholder="例如：保持剧情主线不变，强化人物冲突与情绪递进。"
          @update:model-value="emit('change:instruction', String($event))"
        />
        <AppInput label="目标风格（可选）" :model-value="targetStyle" @update:model-value="emit('change:targetStyle', String($event))" />
        <AppInput
          label="字数上限（可选）"
          :model-value="maxOutputChars"
          placeholder="例如：4500"
          @update:model-value="emit('change:maxOutputChars', String($event))"
        />
      </div>
      <div class="dialog-actions">
        <button type="button" class="btn-submit" :disabled="loading" @click="emit('preview')">{{ loading ? '生成中...' : '预览改写' }}</button>
      </div>
      <div class="dialog-actions">
        <button type="button" class="btn-cancel" :class="{ active: diffMode === 'split' }" @click="emit('change:diffMode', 'split')">分栏对比</button>
        <button type="button" class="btn-cancel" :class="{ active: diffMode === 'unified' }" @click="emit('change:diffMode', 'unified')">行内对比</button>
      </div>
      <div class="panel glass script-rewrite-diff-wrap">
        <p v-if="previewSubtitle" class="muted small">{{ previewSubtitle }}</p>
        <p class="muted small">对比基准：原始剧本</p>
        <ScriptRewriteDiffPanel :original-text="originalText" :rewritten-text="previewText" :mode="diffMode" />
      </div>
      <div class="dialog-actions">
        <button type="button" class="btn-cancel" :disabled="loading || applying" @click="emit('cancel')">取消</button>
        <button type="button" class="btn-confirm" :disabled="loading || applying || !previewText.trim()" @click="emit('apply')">应用改写</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  gap: var(--space-sm);
}
</style>
