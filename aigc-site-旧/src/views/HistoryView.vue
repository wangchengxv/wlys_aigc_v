<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import HistoryCard from '@/components/history/HistoryCard.vue'
import HistoryFilter from '@/components/history/HistoryFilter.vue'
import Pagination from '@/components/history/Pagination.vue'
import { useToast } from '@/composables/useToast'
import { useGenerationStore } from '@/stores/generation'
import type { GenerateMode } from '@/types'

const router = useRouter()
const store = useGenerationStore()
const toast = useToast()

const page = ref(1)
const pageSize = ref(6)
const total = ref(0)
const mode = ref<GenerateMode | 'all'>('all')

async function fetchHistory() {
  const data = await store.loadHistory(page.value, pageSize.value, mode.value)
  total.value = data.total
}

function totalPages() {
  return Math.max(1, Math.ceil(total.value / pageSize.value))
}

async function changePage(next: number) {
  page.value = Math.min(totalPages(), Math.max(1, next))
  await fetchHistory()
}

async function applyFilter() {
  page.value = 1
  await fetchHistory()
}

function openInWorkspace(taskId: string) {
  const task = store.tasks.find((item) => item.taskId === taskId)
  if (task) {
    store.currentTask = task
    router.push('/workspace')
  }
}

async function remove(taskId: string) {
  await store.removeTask(taskId)
  toast.showToast('记录已删除', 'success')
  if (!store.tasks.length && page.value > 1) {
    page.value -= 1
  }
  await fetchHistory()
}

onMounted(fetchHistory)
</script>

<template>
  <section class="panel glass history-page">
    <div class="history-topbar">
      <h2>历史记录</h2>
      <HistoryFilter :mode="mode" @change="mode = $event; applyFilter()" />
    </div>

    <EmptyState
      v-if="!store.tasks.length"
      title="暂无历史记录"
      description="去工作台生成内容后，这里会自动保存你的结果。"
    />
    <TransitionGroup v-else tag="div" class="history-list" name="list">
      <HistoryCard
        v-for="task in store.tasks"
        :key="task.taskId"
        :task="task"
        @open="openInWorkspace(task.taskId)"
        @remove="remove(task.taskId)"
      />
    </TransitionGroup>

    <Pagination
      :page="page"
      :total-pages="totalPages()"
      @change="changePage"
    />
  </section>
</template>

<style scoped>
.history-page {
  padding: var(--space-xl);
}

.history-topbar {
  margin-bottom: var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: var(--space-md);
}

h2 {
  margin: 0;
}

.history-list {
  display: grid;
  gap: var(--space-sm);
}

.list-enter-active,
.list-leave-active {
  transition: all var(--duration-normal);
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (max-width: 600px) {
  .history-page {
    padding: var(--space-lg);
  }

  .history-topbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
