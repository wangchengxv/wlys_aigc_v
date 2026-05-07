<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import PipelineProgressBar from '@/components/script/PipelineProgressBar.vue'
import VideoSegmentCard from '@/components/script/VideoSegmentCard.vue'
import { useToast } from '@/composables/useToast'
import { useScriptProjectStore } from '@/stores/scriptProjects'
import type { VideoSegmentTask } from '@/types'

const route = useRoute()
const toast = useToast()
const store = useScriptProjectStore()
const projectId = computed(() => String(route.params.projectId || ''))

const taskByShot = computed(() => {
  return store.videoTasks.reduce<Record<string, VideoSegmentTask>>((acc, item) => {
    acc[item.shotId] = item
    return acc
  }, {})
})

onMounted(async () => {
  await Promise.all([
    store.loadProject(projectId.value),
    store.loadShots(projectId.value),
    store.loadVideoTasks(projectId.value),
    store.loadPipelineStatus(projectId.value),
  ])
  if (store.pipelineStatus?.projectStatus === 'VIDEO_GENERATING') {
    store.startPolling(projectId.value)
  }
})

onUnmounted(() => {
  store.stopPolling()
})

async function split() {
  try {
    await store.splitShots(projectId.value)
    toast.showToast('镜头拆分完成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '拆分镜头失败', 'error')
  }
}

async function startVideo() {
  try {
    await store.startVideoGeneration(projectId.value)
    toast.showToast('已启动并发视频生成', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '视频生成启动失败', 'error')
  }
}

async function retry(taskId: string) {
  try {
    await store.retryVideoTask(projectId.value, taskId)
    toast.showToast('片段已重新加入队列', 'success')
  } catch (e) {
    toast.showToast(e instanceof Error ? e.message : '重试失败', 'error')
  }
}
</script>

<template>
  <section v-if="store.currentProject" class="page">
    <div class="toolbar panel glass">
      <div>
        <h2>镜头拆分与视频生成</h2>
        <p class="muted">先拆分镜头，再基于已确认关键帧启动并发视频任务。</p>
      </div>
      <div class="actions">
        <AppButton :loading="store.shotLoading" @click="split">拆分镜头</AppButton>
        <AppButton variant="primary" :loading="store.videoLoading" @click="startVideo">启动视频生成</AppButton>
      </div>
    </div>

    <PipelineProgressBar :pipeline="store.pipelineStatus" />

    <LoadingSpinner v-if="store.detailLoading" />
    <div v-else-if="store.shots.length" class="segment-list">
      <VideoSegmentCard
        v-for="shot in store.shots"
        :key="shot.shotId"
        :shot="shot"
        :task="taskByShot[shot.shotId]"
        :busy="store.videoLoading"
        @retry="retry"
      />
    </div>
    <EmptyState
      v-else
      title="还没有镜头"
      description="请先从完善剧本中拆分镜头，系统会为每个镜头生成独立的视频任务。"
    />
  </section>
  <EmptyState v-else title="项目不存在" description="请返回列表重新选择项目。" />
</template>

<style scoped>
.page,
.segment-list {
  display: grid;
  gap: var(--space-lg);
}

.toolbar {
  padding: var(--space-xl);
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

@media (max-width: 760px) {
  .toolbar {
    flex-direction: column;
  }
}
</style>
