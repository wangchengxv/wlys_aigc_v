<script setup lang="ts">
import { onMounted } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ScriptProjectCard from '@/components/script/ScriptProjectCard.vue'
import { useScriptProjectStore } from '@/stores/scriptProjects'

const store = useScriptProjectStore()

onMounted(() => {
  store.loadProjects()
})
</script>

<template>
  <section class="page">
    <div class="head panel glass">
      <div>
        <p class="eyebrow">Script Workflow</p>
        <h2>从剧本到关键帧，再到并发视频</h2>
        <p class="muted">单独维护项目、资产、关键帧和视频任务，不影响原有文生图/文生视频。</p>
      </div>
      <RouterLink class="create-link" to="/script-projects/new">新建项目</RouterLink>
    </div>

    <LoadingSpinner v-if="store.listLoading" />
    <div v-else-if="store.projects.length" class="grid">
      <ScriptProjectCard v-for="item in store.projects" :key="item.projectId" :project="item" />
    </div>
    <EmptyState v-else title="还没有剧本工程" description="先创建一个项目，贴入剧本或上传文档后开始完善与生产。">
      <RouterLink class="create-link" to="/script-projects/new">立即创建</RouterLink>
    </EmptyState>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: var(--space-lg);
}

.head {
  padding: var(--space-xl);
  display: flex;
  justify-content: space-between;
  gap: var(--space-lg);
  align-items: flex-start;
}

.eyebrow,
h2,
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

.create-link {
  text-decoration: none;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background: linear-gradient(145deg, color-mix(in srgb, var(--primary) 92%, #fff), color-mix(in srgb, var(--secondary) 75%, var(--primary)));
  color: var(--btn-primary-fg, #141210);
}

.grid {
  display: grid;
  gap: var(--space-lg);
}

@media (max-width: 760px) {
  .head {
    flex-direction: column;
  }
}
</style>
