<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  artDirectionJson?: string | null
}>()

const emit = defineEmits<{
  (e: 'copied', label: string): void
}>()

const showRaw = ref(false)
const parsed = computed(() => {
  if (!props.artDirectionJson?.trim()) return null
  try {
    return JSON.parse(props.artDirectionJson) as Record<string, unknown>
  } catch {
    return null
  }
})

async function copy(label: string, value: string) {
  try {
    await navigator.clipboard.writeText(value)
    emit('copied', label)
  } catch {
    // ignore
  }
}
</script>

<template>
  <p v-if="!artDirectionJson?.trim()" class="muted">尚未生成美术指导，请点击上方「生成美术指导 B-1」。</p>
  <div v-else-if="!parsed">
    <p class="muted">美术指导 JSON 解析失败，仍可复制原文排查。</p>
    <button type="button" class="pill small" @click="copy('原始 JSON', artDirectionJson || '')">复制原文</button>
    <pre class="art-pre">{{ artDirectionJson }}</pre>
  </div>
  <div v-else class="artd-wrap">
    <div class="actions">
      <button type="button" class="pill small" @click="copy('consistencyAnchors', String(parsed.consistencyAnchors || ''))">复制 Anchors</button>
      <button type="button" class="pill small" @click="showRaw = !showRaw">{{ showRaw ? '隐藏原始 JSON' : '查看原始 JSON' }}</button>
    </div>
    <div class="panel glass">
      <p class="eyebrow">Anchors</p>
      <p>{{ String(parsed.consistencyAnchors || '（无）') }}</p>
    </div>
    <pre v-if="showRaw" class="art-pre">{{ JSON.stringify(parsed, null, 2) }}</pre>
  </div>
</template>
