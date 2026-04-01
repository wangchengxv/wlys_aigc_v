<script setup lang="ts">
import { computed, ref } from 'vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useToast } from '@/composables/useToast'
import { resolveScriptFileUrl } from '@/services/api'
import { useScriptProjectStore } from '@/stores/scriptProjects'
import type { ScriptProjectSummary } from '@/types'

const props = defineProps<{
  project: ScriptProjectSummary
}>()

const { showToast } = useToast()
const store = useScriptProjectStore()
const showDeleteConfirm = ref(false)
const deleting = ref(false)

const coverUrl = computed(() => resolveScriptFileUrl(props.project.coverFileId))

async function handleDeleteConfirm() {
  deleting.value = true
  try {
    await store.removeProject(props.project.projectId)
    showDeleteConfirm.value = false
    showToast('剧本工程已删除', 'success')
  } catch (e) {
    showToast((e as Error)?.message || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <article class="project-card panel glass">
    <div v-if="coverUrl" class="cover-wrap">
      <img class="cover" :src="coverUrl" :alt="project.name" />
    </div>
    <div v-else class="cover placeholder">
      <span>{{ project.name.slice(0, 1) }}</span>
    </div>

    <div class="content">
      <div class="top-line">
        <h3>{{ project.name }}</h3>
        <span class="status">{{ project.status }}</span>
      </div>
      <p class="summary muted">{{ project.scriptSummary || '暂无摘要，创建后可先完善剧本。' }}</p>
      <div class="meta">
        <span>{{ project.visualStyle }}</span>
        <span>{{ project.aspectRatio }}</span>
        <span>{{ project.targetDuration }} 秒</span>
      </div>
      <div class="counts muted">
        <span>资产 {{ project.assetCount }}</span>
        <span>关键帧 {{ project.keyframeCount }}</span>
        <span>视频任务 {{ project.videoTaskCount }}</span>
      </div>
      <div class="footer">
        <span class="muted">{{ formatDate(project.updatedAt) }}</span>
        <div class="links">
          <RouterLink :to="`/script-projects/${project.projectId}`">详情</RouterLink>
          <RouterLink :to="`/script-projects/${project.projectId}/preview`">预览</RouterLink>
          <button
            type="button"
            class="btn-delete"
            :disabled="deleting"
            @click="showDeleteConfirm = true"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <ConfirmDialog
      :visible="showDeleteConfirm"
      title="删除剧本工程"
      message="确定删除该项目吗？剧本、资产与生成记录将一并删除且不可恢复。"
      confirm-text="删除"
      cancel-text="取消"
      @confirm="handleDeleteConfirm"
      @cancel="showDeleteConfirm = false"
    />
  </article>
</template>

<style scoped>
.project-card {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: var(--space-lg);
  overflow: hidden;
}

.cover-wrap,
.cover {
  width: 100%;
  height: 100%;
  min-height: 180px;
}

.cover {
  display: grid;
  place-items: center;
  object-fit: cover;
  background: color-mix(in srgb, var(--primary) 14%, transparent);
}

.placeholder span {
  font-size: 3rem;
  font-family: var(--font-display);
}

.content {
  padding: var(--space-lg) var(--space-lg) var(--space-lg) 0;
  display: grid;
  gap: var(--space-sm);
}

.top-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
}

h3 {
  margin: 0;
  font-size: 1.2rem;
}

.status {
  white-space: nowrap;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 16%, transparent);
  border: 1px solid var(--line);
}

.summary,
.counts {
  margin: 0;
  line-height: 1.7;
}

.meta,
.counts,
.footer,
.links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
}

.meta span {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--tint-primary-08);
  font-size: 12px;
}

.footer {
  justify-content: space-between;
  margin-top: var(--space-sm);
}

.links :deep(a) {
  text-decoration: none;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.btn-delete {
  cursor: pointer;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid color-mix(in srgb, #c62828 35%, var(--line));
  background: color-mix(in srgb, #c62828 12%, transparent);
  color: color-mix(in srgb, #c62828 90%, var(--text-main));
  font-size: inherit;
  font-family: inherit;
}

.btn-delete:hover:not(:disabled) {
  border-color: #c62828;
  background: color-mix(in srgb, #c62828 18%, transparent);
}

.btn-delete:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 800px) {
  .project-card {
    grid-template-columns: 1fr;
  }

  .content {
    padding: 0 var(--space-lg) var(--space-lg);
  }
}
</style>
