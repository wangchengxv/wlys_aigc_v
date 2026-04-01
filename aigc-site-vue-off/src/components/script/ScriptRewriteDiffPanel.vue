<script setup lang="ts">
import { computed } from 'vue'

export type RewriteDiffMode = 'split' | 'unified'

const props = defineProps<{
  originalText: string
  rewrittenText: string
  mode: RewriteDiffMode
}>()

type DiffRow = {
  kind: 'same' | 'add' | 'remove'
  leftText: string
  rightText: string
}

function splitLines(text: string): string[] {
  return (text || '').replace(/\r\n/g, '\n').split('\n')
}

function buildDiffRows(originalText: string, rewrittenText: string): DiffRow[] {
  const left = splitLines(originalText)
  const right = splitLines(rewrittenText)
  const m = left.length
  const n = right.length
  const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))
  for (let i = 1; i <= m; i += 1) {
    for (let j = 1; j <= n; j += 1) {
      if (left[i - 1] === right[j - 1]) dp[i][j] = dp[i - 1][j - 1] + 1
      else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
    }
  }
  const rows: DiffRow[] = []
  let i = m
  let j = n
  while (i > 0 && j > 0) {
    if (left[i - 1] === right[j - 1]) {
      rows.push({ kind: 'same', leftText: left[i - 1], rightText: right[j - 1] })
      i -= 1
      j -= 1
    } else if (dp[i - 1][j] >= dp[i][j - 1]) {
      rows.push({ kind: 'remove', leftText: left[i - 1], rightText: '' })
      i -= 1
    } else {
      rows.push({ kind: 'add', leftText: '', rightText: right[j - 1] })
      j -= 1
    }
  }
  while (i > 0) {
    rows.push({ kind: 'remove', leftText: left[i - 1], rightText: '' })
    i -= 1
  }
  while (j > 0) {
    rows.push({ kind: 'add', leftText: '', rightText: right[j - 1] })
    j -= 1
  }
  return rows.reverse()
}

const rows = computed(() => buildDiffRows(props.originalText, props.rewrittenText))
</script>

<template>
  <div v-if="!originalText.trim()" class="muted">原始稿为空，无法生成对比。</div>
  <div v-else-if="!rewrittenText.trim()" class="muted">暂无改写结果，请先点击“预览改写”。</div>
  <div v-else-if="mode === 'unified'" class="script-rewrite-diff-unified">
    <div v-for="(row, idx) in rows" :key="`${idx}-${row.kind}`" class="script-rewrite-diff-row" :class="row.kind">
      <span class="prefix">{{ row.kind === 'add' ? '+' : row.kind === 'remove' ? '-' : ' ' }}</span>
      <span class="text">{{ row.kind === 'add' ? row.rightText || ' ' : row.leftText || ' ' }}</span>
    </div>
  </div>
  <div v-else class="script-rewrite-diff-split">
    <div class="column">
      <div class="column-title">原始稿</div>
      <div v-for="(row, idx) in rows" :key="`l-${idx}-${row.kind}`" class="script-rewrite-diff-row" :class="row.kind">
        <span class="text">{{ row.leftText || ' ' }}</span>
      </div>
    </div>
    <div class="column">
      <div class="column-title">改写稿</div>
      <div v-for="(row, idx) in rows" :key="`r-${idx}-${row.kind}`" class="script-rewrite-diff-row" :class="row.kind">
        <span class="text">{{ row.rightText || ' ' }}</span>
      </div>
    </div>
  </div>
</template>
