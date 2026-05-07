<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { useToast } from '@/composables/useToast'
import { useScriptProjectStore } from '@/stores/scriptProjects'

const route = useRoute()
const toast = useToast()
const store = useScriptProjectStore()
const projectId = computed(() => String(route.params.projectId || ''))

const activeTab = ref<'original' | 'refined' | 'structured'>('original')
const refinedText = ref('')

watch(
  () => store.scriptPayload?.refinedMarkdown,
  (value) => {
    refinedText.value = value || ''
  },
  { immediate: true },
)

const structuredText = computed(() => JSON.stringify(store.scriptPayload?.structuredScript || {}, null, 2))

async function hydratePage(id: string) {
  await Promise.all([store.loadProject(id), store.loadScript(id)])
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
</script>

<template>
  <section v-if="store.currentProject" class="page">
    <div class="toolbar panel glass">
      <div>
        <h2>{{ store.currentProject.project.name }}</h2>
        <p class="muted">先完善剧本，再进入资产提取与关键帧生成。</p>
      </div>
      <div class="actions">
        <AppButton variant="primary" :loading="store.refineLoading" @click="refine">完善剧本</AppButton>
        <AppButton :loading="store.saveScriptLoading" @click="save">保存修改</AppButton>
        <RouterLink class="nav-btn" :to="`/script-projects/${projectId}/assets`">进入资产页</RouterLink>
      </div>
    </div>

    <div class="tabs panel glass">
      <button :class="{ active: activeTab === 'original' }" @click="activeTab = 'original'">原始剧本</button>
      <button :class="{ active: activeTab === 'refined' }" @click="activeTab = 'refined'">完善剧本</button>
      <button :class="{ active: activeTab === 'structured' }" @click="activeTab = 'structured'">结构化 JSON</button>
    </div>

    <LoadingSpinner v-if="store.detailLoading || !store.scriptPayload" />
    <div v-else class="content panel glass">
      <textarea v-if="activeTab === 'original'" class="editor" :value="store.scriptPayload.originalText" readonly />
      <textarea v-else-if="activeTab === 'refined'" v-model="refinedText" class="editor" />
      <pre v-else class="json">{{ structuredText }}</pre>
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
}
</style>
