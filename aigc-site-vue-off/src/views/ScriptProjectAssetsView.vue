<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AppInput from '@/components/common/AppInput.vue'
import ArtDirectionPreview from '@/components/script/ArtDirectionPreview.vue'
import KeyframeCard from '@/components/script/KeyframeCard.vue'
import { useToast } from '@/composables/useToast'
import { resolveScriptFileUrl } from '@/services/api'
import { useScriptProjectStore } from '@/stores/scriptProjects'
import type { AssetType, ExtractedAsset } from '@/types'

const route = useRoute()
const toast = useToast()
const store = useScriptProjectStore()
const projectId = computed(() => String(route.params.projectId || ''))
const activeTab = ref<AssetType>('CHARACTER')
const artExpanded = ref(true)
const groupSelected = ref<Record<string, boolean>>({})
const groupLocation = ref('')
const groupTime = ref('')
const groupAtmosphere = ref('')
const groupGenerateImage = ref(false)
const lastGroupPrompt = ref('')

const tabToEndpoint: Record<AssetType, 'characters' | 'backgrounds' | 'props'> = {
  CHARACTER: 'characters',
  BACKGROUND: 'backgrounds',
  PROP: 'props',
}

const currentAssets = computed(() => store.assetsByType[activeTab.value] || [])
const characterAssets = computed(() => store.assets.filter((a) => a.assetType === 'CHARACTER'))

onMounted(async () => {
  await Promise.all([
    store.loadProject(projectId.value),
    store.loadAssets(projectId.value),
    store.loadKeyframes(projectId.value),
    store.loadShots(projectId.value),
  ])
})

async function extractCurrent() {
  try {
    await store.extractAssets(projectId.value, tabToEndpoint[activeTab.value])
    toast.showToast('资产抽取完成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '抽取失败', 'error')
  }
}

async function saveAsset(asset: ExtractedAsset) {
  try {
    await store.saveAsset(projectId.value, asset.assetId, {
      name: asset.name,
      description: asset.description,
      promptDraft: asset.promptDraft,
      tags: asset.tags,
      metadata: asset.metadata,
    })
    toast.showToast('资产已保存', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '保存失败', 'error')
  }
}

async function generate(assetId: string) {
  try {
    await store.generateKeyframes(projectId.value, assetId)
    toast.showToast('关键帧生成完成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '关键帧生成失败', 'error')
  }
}

async function confirm(keyframeId: string) {
  try {
    await store.confirmKeyframe(projectId.value, keyframeId)
    toast.showToast('已确认关键帧', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '确认失败', 'error')
  }
}

async function regenerate(keyframeId: string) {
  try {
    await store.regenerateKeyframe(projectId.value, keyframeId)
    toast.showToast('关键帧已重生成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '重生成失败', 'error')
  }
}

async function generateArtDirection() {
  try {
    await store.generateArtDirectionAction(projectId.value)
    toast.showToast('美术指导已生成（B-1）', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '生成失败', 'error')
  }
}

async function batchCharacters() {
  try {
    await store.batchCharacterVisualPrompts(projectId.value)
    toast.showToast('批量角色视觉提示词已生成（B-2）', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '批量生成失败', 'error')
  }
}

async function visualPrompt(assetId: string) {
  try {
    await store.generateVisualPromptForAsset(projectId.value, assetId)
    toast.showToast('视觉提示词已更新', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '生成失败', 'error')
  }
}

async function turnaroundPlan(assetId: string) {
  try {
    await store.generateTurnaroundPlanForAsset(projectId.value, assetId)
    toast.showToast('九宫格视角规划已生成（B-6）', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '规划失败', 'error')
  }
}

async function turnaroundImage(assetId: string) {
  try {
    await store.generateTurnaroundImageForAsset(projectId.value, assetId)
    toast.showToast('九宫格造型图已生成（B-7）', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '生成失败', 'error')
  }
}

async function threeView(assetId: string) {
  try {
    await store.generateThreeViewForAsset(projectId.value, assetId)
    toast.showToast('三视图已生成（正/侧/背）', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '三视图生成失败', 'error')
  }
}

async function storyboardPlan(assetId: string) {
  try {
    await store.generateStoryboardPlanForAsset(projectId.value, assetId)
    toast.showToast('九宫格分镜规划已生成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '生成九宫格分镜规划失败', 'error')
  }
}

async function storyboardTranslate(assetId: string) {
  try {
    await store.translateStoryboardPlanForAsset(projectId.value, assetId)
    toast.showToast('九宫格分镜翻译已更新', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '翻译失败', 'error')
  }
}

async function storyboardImage(assetId: string) {
  try {
    await store.generateStoryboardImageForAsset(projectId.value, assetId)
    toast.showToast('九宫格分镜图已生成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '出图失败', 'error')
  }
}

async function groupScene() {
  const ids = characterAssets.value.filter((a) => groupSelected.value[a.assetId]).map((a) => a.assetId)
  if (ids.length < 1) {
    toast.showToast('请至少勾选一个角色', 'error')
    return
  }
  try {
    const res = await store.generateGroupScenePrompt(projectId.value, {
      characterAssetIds: ids,
      location: groupLocation.value.trim() || undefined,
      time: groupTime.value.trim() || undefined,
      atmosphere: groupAtmosphere.value.trim() || undefined,
      generateImage: groupGenerateImage.value,
    })
    lastGroupPrompt.value = res.promptText
    toast.showToast(groupGenerateImage.value && res.imageFileId ? '群像提示词与概念图已生成（B-8）' : '群像提示词已生成（B-8）', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '群像生成失败', 'error')
  }
}

function updateTags(asset: ExtractedAsset, value: string | number) {
  asset.tags = String(value)
    .split(/[，,、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}
</script>

<template>
  <section v-if="store.currentProject" class="page">
    <div class="toolbar panel glass">
      <div>
        <h2>资产与关键帧</h2>
        <p class="muted">分别抽取角色、背景、道具，并为每项生成可确认的关键帧。</p>
      </div>
      <div class="actions">
        <AppButton variant="primary" :loading="store.assetLoading" @click="extractCurrent">抽取当前分类</AppButton>
        <AppButton :loading="store.artDirectionLoading" @click="generateArtDirection">生成美术指导 B-1</AppButton>
        <RouterLink class="nav-btn" :to="`/script-projects/${projectId}/video`">进入视频页</RouterLink>
      </div>
    </div>

    <div class="panel glass art-direction-panel">
      <button type="button" class="art-toggle" @click="artExpanded = !artExpanded">{{ artExpanded ? '▼' : '▶' }} 全局美术指导（B-1）</button>
      <div v-if="artExpanded" class="art-direction-body">
        <ArtDirectionPreview :art-direction-json="store.currentProject?.project.artDirectionJson ?? null" @copied="toast.showToast(`${$event} 已复制`, 'success')" />
      </div>
    </div>

    <div class="panel glass group-scene-panel">
      <h3>多角色群像 B-8</h3>
      <p class="muted">勾选多个角色后可生成群像提示词（可选同时出概念图）。</p>
      <div class="group-character-picks">
        <label v-for="a in characterAssets" :key="a.assetId" class="group-check">
          <input v-model="groupSelected[a.assetId]" type="checkbox" />
          {{ a.name }}
        </label>
      </div>
      <div class="form-grid">
        <AppInput v-model="groupLocation" label="地点" placeholder="例如：废弃仓库二楼" />
        <AppInput v-model="groupTime" label="时间" placeholder="例如：深夜" />
        <AppInput v-model="groupAtmosphere" label="氛围" placeholder="例如：紧张、雨夜" />
      </div>
      <label class="group-check"><input v-model="groupGenerateImage" type="checkbox" /> 同时生成概念图</label>
      <AppButton variant="primary" :loading="store.groupSceneLoading" @click="groupScene">生成群像提示词</AppButton>
      <div v-if="lastGroupPrompt" class="visual-prompt-block">
        <p class="eyebrow">上次群像提示词</p>
        <p class="visual-prompt-text">{{ lastGroupPrompt }}</p>
      </div>
    </div>

    <div class="tabs panel glass">
      <button :class="{ active: activeTab === 'CHARACTER' }" @click="activeTab = 'CHARACTER'">人物形象</button>
      <button :class="{ active: activeTab === 'BACKGROUND' }" @click="activeTab = 'BACKGROUND'">视频背景</button>
      <button :class="{ active: activeTab === 'PROP' }" @click="activeTab = 'PROP'">视频道具</button>
    </div>

    <div v-if="activeTab === 'CHARACTER' && characterAssets.length > 0" class="panel glass batch-bar">
      <AppButton :loading="store.visualPromptLoading" @click="batchCharacters">批量生成角色视觉提示词 B-2</AppButton>
      <span class="muted">需先完成 B-1 美术指导</span>
    </div>
    <LoadingSpinner v-if="store.assetLoading && !store.assets.length" />
    <div v-else-if="currentAssets.length" class="asset-list">
      <article v-for="asset in currentAssets" :key="asset.assetId" class="asset-card panel glass">
        <div class="asset-head">
          <div>
            <h3>{{ asset.name }}</h3>
            <p class="muted">状态：{{ asset.status }}</p>
          </div>
          <div class="actions">
            <AppButton size="sm" :loading="store.assetLoading" @click="saveAsset(asset)">保存</AppButton>
            <AppButton size="sm" :loading="store.visualPromptLoading" @click="visualPrompt(asset.assetId)">生成视觉提示词</AppButton>
            <AppButton size="sm" :loading="store.visualPromptLoading" @click="threeView(asset.assetId)">生成三视图</AppButton>
            <AppButton v-if="asset.assetType === 'CHARACTER'" size="sm" :loading="store.visualPromptLoading" @click="turnaroundPlan(asset.assetId)">
              九宫格规划 B-6
            </AppButton>
            <AppButton
              v-if="asset.assetType === 'CHARACTER'"
              size="sm"
              variant="primary"
              :loading="store.visualPromptLoading"
              @click="turnaroundImage(asset.assetId)"
            >
              九宫格造型图 B-7
            </AppButton>
            <AppButton v-if="asset.assetType === 'CHARACTER'" size="sm" :loading="store.visualPromptLoading" @click="storyboardPlan(asset.assetId)">
              分镜九宫格规划
            </AppButton>
            <AppButton v-if="asset.assetType === 'CHARACTER'" size="sm" :loading="store.visualPromptLoading" @click="storyboardTranslate(asset.assetId)">
              重译中文
            </AppButton>
            <AppButton v-if="asset.assetType === 'CHARACTER'" size="sm" :loading="store.visualPromptLoading" @click="storyboardImage(asset.assetId)">
              分镜九宫格出图
            </AppButton>
            <AppButton size="sm" variant="primary" :loading="store.keyframeLoading" @click="generate(asset.assetId)">
              生成关键帧
            </AppButton>
          </div>
        </div>

        <div class="form-grid">
          <AppInput v-model="asset.name" label="名称" />
          <AppInput
            :model-value="asset.tags.join(', ')"
            label="标签"
            placeholder="例如：主角, 正面像, 暖光"
            @update:model-value="updateTags(asset, $event)"
          />
        </div>
        <AppInput v-model="asset.description" label="描述" as="textarea" :rows="4" />
        <AppInput v-model="asset.promptDraft" label="关键帧提示词草稿" as="textarea" :rows="4" />
        <div v-if="asset.visualPrompt" class="visual-prompt-block">
          <div class="visual-prompt-head">
            <span class="eyebrow">视觉提示词（B-3/4/5）</span>
          </div>
          <p class="visual-prompt-text">{{ asset.visualPrompt }}</p>
        </div>
        <div v-if="asset.turnaroundImageFileId" class="turnaround-preview">
          <p class="eyebrow">九宫格造型 B-7</p>
          <img class="turnaround-img" :src="resolveScriptFileUrl(asset.turnaroundImageFileId)" :alt="`${asset.name} 九宫格`" />
        </div>
        <div v-if="asset.storyboardImageFileId" class="turnaround-preview">
          <p class="eyebrow">分镜九宫格</p>
          <img class="turnaround-img" :src="resolveScriptFileUrl(asset.storyboardImageFileId)" :alt="`${asset.name} 分镜九宫格`" />
        </div>
        <div v-if="asset.threeViewImageFileId" class="turnaround-preview">
          <p class="eyebrow">三视图（正/侧/背）</p>
          <img class="turnaround-img" :src="resolveScriptFileUrl(asset.threeViewImageFileId)" :alt="`${asset.name} 三视图`" />
        </div>

        <div v-if="store.keyframesByAsset[asset.assetId]?.length" class="keyframe-grid">
          <KeyframeCard
            v-for="item in store.keyframesByAsset[asset.assetId]"
            :key="item.keyframeId"
            :item="item"
            :busy="store.keyframeLoading"
            @confirm="confirm"
            @regenerate="regenerate"
          />
        </div>
        <p v-else class="muted">还没有关键帧，保存后可直接生成。</p>
      </article>
    </div>
    <EmptyState
      v-else
      title="当前分类还没有资产"
      description="点击上方按钮抽取当前分类的视觉资产，然后再生成关键帧。"
    />
  </section>
  <EmptyState v-else title="项目不存在" description="请返回列表重新选择项目。" />
</template>

<style scoped>
.page,
.asset-list {
  display: grid;
  gap: var(--space-lg);
}

.turnaround-preview {
  display: grid;
  gap: var(--space-sm);
}

.turnaround-img {
  width: 100%;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
}

.visual-prompt-block {
  border: 1px dashed var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-md);
}

.visual-prompt-text {
  white-space: pre-wrap;
  margin: 0;
  line-height: 1.65;
}

.toolbar,
.tabs,
.asset-card {
  padding: var(--space-xl);
}

.toolbar,
.asset-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-lg);
  align-items: flex-start;
}

h2,
h3,
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

.asset-card {
  display: grid;
  gap: var(--space-md);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.keyframe-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

@media (max-width: 760px) {
  .toolbar,
  .asset-head {
    flex-direction: column;
  }

  .form-grid,
  .keyframe-grid {
    grid-template-columns: 1fr;
  }
}
</style>
