<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PipelineProgressBar from '@/components/script/PipelineProgressBar.vue'
import { useToast } from '@/composables/useToast'
import { useScriptProjectStore } from '@/stores/scriptProjects'

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()
const store = useScriptProjectStore()
const projectId = computed(() => String(route.params.projectId || ''))

const showDeleteConfirm = ref(false)
const deleting = ref(false)

onMounted(async () => {
  await Promise.all([store.loadProject(projectId.value), store.loadPipelineStatus(projectId.value)])
})

async function handleDeleteConfirm() {
  const id = projectId.value
  deleting.value = true
  try {
    await store.removeProject(id)
    store.stopPolling()
    showDeleteConfirm.value = false
    showToast('剧本项目已删除', 'success')
    await router.push('/script-projects')
  } catch (e) {
    showToast((e as Error)?.message || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <section v-if="store.currentProject" class="page">
    <div class="hero panel glass">
      <div>
        <p class="eyebrow">Project</p>
        <h2>{{ store.currentProject.project.name }}</h2>
        <p class="muted">{{ store.currentProject.project.scriptSummary || '暂无摘要' }}</p>
      </div>
      <div class="actions">
        <RouterLink class="nav-btn primary" :to="`/script-projects/${projectId}/preview`">剧本预览</RouterLink>
        <RouterLink class="nav-btn" :to="`/script-projects/${projectId}/assets`">资产与关键帧</RouterLink>
        <RouterLink class="nav-btn" :to="`/script-projects/${projectId}/video`">视频生成</RouterLink>
        <button
          type="button"
          class="nav-btn danger"
          :disabled="deleting"
          @click="showDeleteConfirm = true"
        >
          删除剧本
        </button>
      </div>
    </div>

    <ConfirmDialog
      :visible="showDeleteConfirm"
      title="删除剧本项目"
      message="确定删除该项目吗？剧本、资产与生成记录将一并删除且不可恢复。"
      confirm-text="删除"
      cancel-text="取消"
      @confirm="handleDeleteConfirm"
      @cancel="showDeleteConfirm = false"
    />

    <div class="meta-grid">
      <div class="panel glass meta-card">
        <h3>基础信息</h3>
        <p>状态：{{ store.currentProject.project.status }}</p>
        <p>风格：{{ store.currentProject.project.visualStyle }}</p>
        <p>比例：{{ store.currentProject.project.aspectRatio }}</p>
        <p>时长：{{ store.currentProject.project.targetDuration }} 秒</p>
        <p>语言：{{ store.currentProject.project.language }}</p>
      </div>
      <div class="panel glass meta-card">
        <h3>资产统计</h3>
        <p>文档版本：{{ store.currentProject.documents.length }}</p>
        <p>已抽取资产：{{ store.currentProject.assets.length }}</p>
        <p>关键帧：{{ store.currentProject.keyframes.length }}</p>
        <p>镜头：{{ store.currentProject.shots.length }}</p>
        <p>视频任务：{{ store.currentProject.videoTasks.length }}</p>
      </div>
    </div>

    <PipelineProgressBar :pipeline="store.pipelineStatus" />
  </section>
  <EmptyState v-else title="项目不存在" description="请返回列表重新选择项目。">
    <RouterLink to="/script-projects">返回项目列表</RouterLink>
  </EmptyState>
</template>

<style scoped>
.page {
  display: grid;
  gap: var(--space-lg);
}

.hero,
.meta-card {
  padding: var(--space-xl);
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: var(--space-lg);
  align-items: flex-start;
}

.eyebrow,
h2,
h3,
p {
  margin: 0;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-muted);
}

h2 {
  margin: 6px 0 8px;
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

.nav-btn.primary {
  color: var(--btn-primary-fg, #141210);
  border-color: transparent;
  background: linear-gradient(145deg, color-mix(in srgb, var(--primary) 92%, #fff), color-mix(in srgb, var(--secondary) 75%, var(--primary)));
}

.nav-btn.danger {
  border-color: color-mix(in srgb, #c62828 35%, var(--line));
  background: color-mix(in srgb, #c62828 12%, transparent);
  color: color-mix(in srgb, #c62828 90%, var(--text-main));
}

.nav-btn.danger:hover:not(:disabled) {
  border-color: #c62828;
  background: color-mix(in srgb, #c62828 18%, transparent);
}

.nav-btn.danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-lg);
}

.meta-card {
  display: grid;
  gap: var(--space-sm);
}

@media (max-width: 760px) {
  .hero,
  .meta-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
