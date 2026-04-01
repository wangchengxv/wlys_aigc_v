<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ScriptAppendPreviewDialog from '@/components/script/ScriptAppendPreviewDialog.vue'
import ScriptRewriteDialog from '@/components/script/ScriptRewriteDialog.vue'
import ScriptRevisionPanel from '@/components/script/ScriptRevisionPanel.vue'
import ScriptStructuredPreview from '@/components/script/ScriptStructuredPreview.vue'
import { useToast } from '@/composables/useToast'
import { useScriptProjectStore } from '@/stores/scriptProjects'

const route = useRoute()
const toast = useToast()
const store = useScriptProjectStore()
const projectId = computed(() => String(route.params.projectId || ''))

const activeTab = ref<'original' | 'refined' | 'structured' | 'json'>('original')
const structuredSub = ref<'overview' | 'scenes' | 'characters' | 'props'>('overview')
const refinedText = ref('')
const briefPrompt = ref('')
const showAppendDialog = ref(false)
const appendPreviewText = ref('')
const appendPreviewSubtitle = ref('')
const showRewriteDialog = ref(false)
const rewriteInstruction = ref('')
const rewriteTargetStyle = ref('')
const rewriteMaxOutputChars = ref('')
const rewritePreviewText = ref('')
const rewriteDiffMode = ref<'split' | 'unified'>('split')
const rewritePreviewSubtitle = ref('')

watch(
  () => store.scriptPayload?.refinedMarkdown,
  (value) => {
    refinedText.value = value || ''
  },
  { immediate: true },
)

const structuredText = computed(() => JSON.stringify(store.scriptPayload?.structuredScript || {}, null, 2))

async function hydratePage(id: string) {
  await Promise.all([store.loadProject(id), store.loadScript(id), store.loadRevisions(id)])
}

onMounted(async () => {
  await hydratePage(projectId.value)
})

watch(
  projectId,
  async (id, prev) => {
    if (!id || id === prev) return
    await hydratePage(id)
    activeTab.value = 'original'
  },
)

async function refine() {
  try {
    await store.refine(projectId.value)
    toast.showToast('剧本完善完成', 'success')
    activeTab.value = 'refined'
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
  }
}

async function refineWithPrompt() {
  const prompt = briefPrompt.value.trim()
  if (!prompt) {
    toast.showToast('请输入短提示词', 'error')
    return
  }
  try {
    await store.refineWithPrompt(projectId.value, prompt)
    toast.showToast('剧本完善功能完成', 'success')
    activeTab.value = 'structured'
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
  }
}

async function save() {
  try {
    await store.saveScript(projectId.value, {
      refinedMarkdown: refinedText.value,
      structuredScript: store.scriptPayload?.structuredScript || {},
    })
    toast.showToast('剧本保存成功', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '保存失败', 'error')
  }
}

async function runOptimize(kind: 'scenes' | 'characters' | 'props') {
  try {
    if (kind === 'scenes') await store.optimizeScenes(projectId.value)
    if (kind === 'characters') await store.optimizeCharacters(projectId.value)
    if (kind === 'props') await store.optimizeProps(projectId.value)
    toast.showToast('智能体优化完成', 'success')
    activeTab.value = 'structured'
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '优化失败', 'error')
  }
}

async function handleAppendPreview() {
  try {
    const res = await store.appendPreview(projectId.value, {})
    appendPreviewText.value = res.appendText || ''
    appendPreviewSubtitle.value = `基于：${res.baseUsed}｜已有 ${res.existingLength} 字符｜本次上限 ${res.maxAppendChars} 字符`
    showAppendDialog.value = true
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '续写预览失败', 'error')
  }
}

async function handleAppendConfirm() {
  try {
    const base =
      (store.scriptPayload?.refinedMarkdown || '').trim() !== ''
        ? store.scriptPayload?.refinedMarkdown || ''
        : store.scriptPayload?.originalText || ''
    const combined = `${base}${base.endsWith('\n') ? '' : '\n'}${appendPreviewText.value}`.trim()
    await store.saveScript(projectId.value, {
      refinedMarkdown: combined,
      structuredScript: store.scriptPayload?.structuredScript || {},
    })
    showAppendDialog.value = false
    activeTab.value = 'refined'
    toast.showToast('续写已追加并保存', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '保存失败', 'error')
  }
}

async function handleRewritePreview() {
  const instruction = rewriteInstruction.value.trim()
  if (!instruction) {
    toast.showToast('请先输入改写要求', 'error')
    return
  }
  const maxOutputChars = rewriteMaxOutputChars.value.trim() ? Number(rewriteMaxOutputChars.value.trim()) : undefined
  if (maxOutputChars != null && (!Number.isFinite(maxOutputChars) || maxOutputChars <= 0)) {
    toast.showToast('字数上限必须为正整数', 'error')
    return
  }
  try {
    const result = await store.rewritePreview(projectId.value, {
      rewriteInstruction: instruction,
      targetStyle: rewriteTargetStyle.value.trim() || undefined,
      maxOutputChars,
      language: store.currentProject?.project.language || undefined,
    })
    rewritePreviewText.value = result.rewrittenText || ''
    rewritePreviewSubtitle.value = `基于：${result.baseUsed}｜原文长度 ${result.sourceLength} 字符${
      result.maxOutputChars ? `｜本次上限 ${result.maxOutputChars} 字符` : ''
    }`
    toast.showToast('改写预览生成成功', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '改写预览失败', 'error')
  }
}

async function handleApplyRewrite() {
  if (!rewritePreviewText.value.trim()) {
    toast.showToast('请先生成改写预览', 'error')
    return
  }
  try {
    await store.applyRewrite(projectId.value, { rewrittenText: rewritePreviewText.value })
    refinedText.value = rewritePreviewText.value
    showRewriteDialog.value = false
    activeTab.value = 'refined'
    toast.showToast('改写结果已应用并保存', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '应用改写失败', 'error')
  }
}

async function handleRestoreRevision(revisionId: string) {
  try {
    await store.restoreRevision(projectId.value, revisionId)
    toast.showToast('已恢复到所选版本', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '恢复失败', 'error')
  }
}
</script>

<template>
  <section v-if="store.currentProject" class="page">
    <div class="toolbar panel glass">
      <div>
        <h2>{{ store.currentProject.project.name }}</h2>
        <p class="muted">先完善剧本，再进入资产提取与关键帧生成。</p>
      </div>
      <div class="actions script-preview-toolbar-actions">
        <AppButton variant="primary" :loading="store.refineLoading" @click="refine">完善剧本</AppButton>
        <AppButton variant="primary" :loading="store.appendLoading" @click="handleAppendPreview">AI 续写剧本</AppButton>
        <AppButton variant="primary" :loading="store.rewriteLoading" @click="showRewriteDialog = true">AI 剧本改写</AppButton>
        <div style="width: min(360px, 100%)">
          <AppInput
            :model-value="briefPrompt"
            label="短提示词"
            as="textarea"
            :rows="3"
            placeholder="例如：让节奏更紧凑，突出主角情绪变化。"
            @update:model-value="briefPrompt = String($event)"
          />
        </div>
        <AppButton variant="primary" :loading="store.refinePromptLoading" @click="refineWithPrompt">剧本完善功能</AppButton>
        <AppButton :loading="store.saveScriptLoading" @click="save">保存修改</AppButton>
        <RouterLink class="nav-btn" :to="`/script-projects/${projectId}/assets`">进入资产页</RouterLink>
      </div>
    </div>

    <ScriptAppendPreviewDialog
      :visible="showAppendDialog"
      :append-text="appendPreviewText"
      :subtitle="appendPreviewSubtitle"
      :loading="store.saveScriptLoading"
      @cancel="showAppendDialog = false"
      @confirm="handleAppendConfirm"
    />

    <ScriptRewriteDialog
      :visible="showRewriteDialog"
      :instruction="rewriteInstruction"
      :target-style="rewriteTargetStyle"
      :max-output-chars="rewriteMaxOutputChars"
      :original-text="store.scriptPayload?.originalText || ''"
      :preview-text="rewritePreviewText"
      :diff-mode="rewriteDiffMode"
      :loading="store.rewriteLoading"
      :applying="store.rewriteLoading"
      :preview-subtitle="rewritePreviewSubtitle"
      @change:instruction="rewriteInstruction = $event"
      @change:target-style="rewriteTargetStyle = $event"
      @change:max-output-chars="rewriteMaxOutputChars = $event"
      @change:diff-mode="rewriteDiffMode = $event"
      @cancel="showRewriteDialog = false"
      @preview="handleRewritePreview"
      @apply="handleApplyRewrite"
    />

    <div class="panel glass script-optimize-bar">
      <span class="eyebrow">三阶段优化</span>
      <div class="script-optimize-actions">
        <AppButton size="sm" :loading="store.optimizeLoading" @click="runOptimize('scenes')">场景 / 场次</AppButton>
        <AppButton size="sm" :loading="store.optimizeLoading" @click="runOptimize('characters')">角色人设</AppButton>
        <AppButton size="sm" :loading="store.optimizeLoading" @click="runOptimize('props')">道具巧思</AppButton>
      </div>
      <p class="muted small">需先完成「完善剧本」。顺序建议：场景 → 角色 → 道具。</p>
    </div>

    <div class="script-preview-layout">
      <div class="script-preview-main">
        <div class="tabs panel glass">
          <button :class="{ active: activeTab === 'original' }" @click="activeTab = 'original'">原始剧本</button>
          <button :class="{ active: activeTab === 'refined' }" @click="activeTab = 'refined'">完善剧本</button>
          <button :class="{ active: activeTab === 'structured' }" @click="activeTab = 'structured'">结构化预览</button>
          <button :class="{ active: activeTab === 'json' }" @click="activeTab = 'json'">JSON</button>
        </div>

        <LoadingSpinner v-if="store.detailLoading || !store.scriptPayload" />
        <div v-else class="content panel glass">
          <textarea v-if="activeTab === 'original'" class="editor" :value="store.scriptPayload.originalText" readonly />
          <textarea v-else-if="activeTab === 'refined'" v-model="refinedText" class="editor" />
          <div v-else-if="activeTab === 'structured'" class="script-structured-wrap">
            <div class="script-structured-subtabs">
              <button :class="{ active: structuredSub === 'overview' }" @click="structuredSub = 'overview'">概览</button>
              <button :class="{ active: structuredSub === 'scenes' }" @click="structuredSub = 'scenes'">场次</button>
              <button :class="{ active: structuredSub === 'characters' }" @click="structuredSub = 'characters'">角色</button>
              <button :class="{ active: structuredSub === 'props' }" @click="structuredSub = 'props'">道具</button>
            </div>
            <ScriptStructuredPreview :structured="store.scriptPayload.structuredScript || {}" :sub-view="structuredSub" />
          </div>
          <pre v-else class="json">{{ structuredText }}</pre>
        </div>
      </div>
      <aside class="panel glass script-revision-aside">
        <h3>历史版本</h3>
        <ScriptRevisionPanel
          :revisions="store.revisions"
          :loading="store.revisionLoading"
          :restoring-id="store.restoringRevisionId"
          @restore="handleRestoreRevision"
        />
      </aside>
    </div>
  </section>
  <EmptyState v-else title="项目不存在" description="请返回列表重新选择项目。" />
</template>

<style scoped>
.page {
  display: grid;
  gap: var(--space-lg);
}

.toolbar,
.tabs,
.content {
  padding: var(--space-xl);
}

.script-preview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: var(--space-lg);
}

.script-preview-main {
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: var(--space-lg);
  align-items: flex-start;
}

h2,
p {
  margin: 0;
}

.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.nav-btn {
  text-decoration: none;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.tabs {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.tabs button {
  min-height: 42px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  color: var(--text-main);
}

.tabs button.active {
  background: color-mix(in srgb, var(--primary) 18%, transparent);
}

.script-structured-subtabs {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}

.script-structured-subtabs button {
  min-height: 34px;
  padding: 0 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: transparent;
}

.script-structured-subtabs button.active {
  background: var(--tint-primary-14);
}

.editor,
.json {
  width: 100%;
  min-height: 560px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  background: var(--field-bg);
  color: var(--text-main);
  line-height: 1.7;
}

.json {
  overflow: auto;
  margin: 0;
}

@media (max-width: 760px) {
  .toolbar {
    flex-direction: column;
  }

  .script-preview-layout {
    grid-template-columns: 1fr;
  }
}
</style>
