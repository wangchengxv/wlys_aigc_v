<script setup lang="ts">
const props = defineProps<{
  structured: Record<string, unknown>
  subView: 'overview' | 'scenes' | 'characters' | 'props'
}>()

function asRecordList(value: unknown): Record<string, unknown>[] {
  if (!Array.isArray(value)) return []
  return value.filter((item): item is Record<string, unknown> => item != null && typeof item === 'object')
}

function str(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'string') return v
  if (typeof v === 'number' || typeof v === 'boolean') return String(v)
  try {
    return JSON.stringify(v)
  } catch {
    return String(v)
  }
}

const scenes = asRecordList(props.structured.scenes)
const segments = asRecordList(props.structured.segments)
const characters = asRecordList(props.structured.characters)
const propsList = asRecordList(props.structured.props)
</script>

<template>
  <div v-if="subView === 'overview'" class="script-structured-preview">
    <div class="script-preview-block">
      <h4>标题</h4>
      <p>{{ str(structured.title) || '—' }}</p>
    </div>
    <div class="script-preview-block">
      <h4>摘要</h4>
      <p class="script-preview-body">{{ str(structured.summary) || '—' }}</p>
    </div>
    <div class="script-preview-stats muted">
      <span>场次 {{ scenes.length }}</span>
      <span>分段 {{ segments.length }}</span>
      <span>角色 {{ characters.length }}</span>
      <span>道具 {{ propsList.length }}</span>
    </div>
  </div>
  <div v-else-if="subView === 'scenes'" class="script-structured-preview">
    <p v-if="scenes.length === 0" class="muted">暂无场次数据，请先完善剧本。</p>
    <article v-for="(scene, i) in scenes" :key="str(scene.id) || String(i)" class="script-preview-card">
      <header>
        <strong>{{ str(scene.title) || `场景 ${i + 1}` }}</strong>
        <span class="muted">{{ str(scene.location) }} · {{ str(scene.time) }}</span>
      </header>
      <p>{{ str(scene.summary) }}</p>
    </article>
  </div>
  <div v-else-if="subView === 'characters'" class="script-structured-preview">
    <p v-if="characters.length === 0" class="muted">暂无角色数据。</p>
    <article v-for="(c, i) in characters" :key="str(c.id) || String(i)" class="script-preview-card">
      <h4>{{ str(c.name) || `角色 ${i + 1}` }}</h4>
      <p>{{ str(c.description) }}</p>
    </article>
  </div>
  <div v-else class="script-structured-preview">
    <p v-if="propsList.length === 0" class="muted">暂无道具数据。</p>
    <article v-for="(p, i) in propsList" :key="str(p.id) || String(i)" class="script-preview-card">
      <h4>{{ str(p.name) || `道具 ${i + 1}` }}</h4>
      <p>{{ str(p.description) }}</p>
    </article>
  </div>
</template>
