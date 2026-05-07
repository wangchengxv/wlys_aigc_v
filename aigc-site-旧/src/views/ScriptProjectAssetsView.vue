<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AppInput from '@/components/common/AppInput.vue'
import KeyframeCard from '@/components/script/KeyframeCard.vue'
import { useToast } from '@/composables/useToast'
import { useScriptProjectStore } from '@/stores/scriptProjects'
import type { AssetType, ExtractedAsset } from '@/types'

const route = useRoute()
const toast = useToast()
const store = useScriptProjectStore()
const projectId = computed(() => String(route.params.projectId || ''))
const activeTab = ref<AssetType>('CHARACTER')

const tabToEndpoint: Record<AssetType, 'characters' | 'backgrounds' | 'props'> = {
  CHARACTER: 'characters',
  BACKGROUND: 'backgrounds',
  PROP: 'props',
}

const currentAssets = computed(() => store.assetsByType[activeTab.value] || [])

onMounted(async () => {
  await Promise.all([store.loadProject(projectId.value), store.loadAssets(projectId.value), store.loadKeyframes(projectId.value)])
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
        <RouterLink class="nav-btn" :to="`/script-projects/${projectId}/video`">进入视频页</RouterLink>
      </div>
    </div>

    <div class="tabs panel glass">
      <button :class="{ active: activeTab === 'CHARACTER' }" @click="activeTab = 'CHARACTER'">人物形象</button>
      <button :class="{ active: activeTab === 'BACKGROUND' }" @click="activeTab = 'BACKGROUND'">视频背景</button>
      <button :class="{ active: activeTab === 'PROP' }" @click="activeTab = 'PROP'">视频道具</button>
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
