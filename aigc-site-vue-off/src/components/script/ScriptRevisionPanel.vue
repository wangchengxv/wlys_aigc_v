<script setup lang="ts">
import AppButton from '@/components/common/AppButton.vue'
import type { ScriptRevision } from '@/types'

defineProps<{
  revisions: ScriptRevision[]
  loading: boolean
  restoringId: string | null
}>()

const emit = defineEmits<{
  (e: 'restore', revisionId: string): void
}>()

const kindLabel: Record<string, string> = {
  REFINE: '完善前',
  USER_EDIT: '编辑前',
  OPTIMIZE_SCENE: '场景优化前',
  OPTIMIZE_CHARACTER: '角色优化前',
  OPTIMIZE_PROP: '道具优化前',
  RESTORE: '恢复前',
  IMPORT: '导入前',
  BEFORE_UPDATE: '更新前',
}
</script>

<template>
  <p v-if="loading && revisions.length === 0" class="muted">加载修订记录...</p>
  <p v-else-if="revisions.length === 0" class="muted">暂无修订快照（保存或覆盖完善稿后会自动生成）。</p>
  <ul v-else class="script-revision-list">
    <li v-for="r in revisions" :key="r.revisionId" class="script-revision-item">
      <div>
        <span class="script-revision-kind">{{ kindLabel[r.kind] || r.kind }}</span>
        <span class="muted">#{{ r.revisionIndex }} · {{ r.label }} · {{ new Date(r.createdAt).toLocaleString() }}</span>
      </div>
      <AppButton variant="ghost" :disabled="restoringId === r.revisionId" :loading="restoringId === r.revisionId" @click="emit('restore', r.revisionId)">
        恢复到此版本
      </AppButton>
    </li>
  </ul>
</template>
