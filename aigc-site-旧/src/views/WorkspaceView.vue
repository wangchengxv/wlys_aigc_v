<script setup lang="ts">
import PromptPanel from '@/components/workspace/PromptPanel.vue'
import ResultPanel from '@/components/workspace/ResultPanel.vue'
import { useToast } from '@/composables/useToast'

const toast = useToast()

function onGenerated() {
  toast.showToast('生成成功', 'success')
}
</script>

<template>
  <section class="workspace-grid">
    <PromptPanel @generated="onGenerated" />
    <ResultPanel />
  </section>
</template>

<style scoped>
.workspace-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

@media (max-width: 1200px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
