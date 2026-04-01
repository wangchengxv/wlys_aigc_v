import { useMemo } from 'react'

export type RewriteDiffMode = 'split' | 'unified'

type DiffRow = {
  kind: 'same' | 'add' | 'remove'
  leftText: string
  rightText: string
}

type Props = {
  originalText: string
  rewrittenText: string
  mode: RewriteDiffMode
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

export function ScriptRewriteDiffPanel({ originalText, rewrittenText, mode }: Props) {
  const rows = useMemo(() => buildDiffRows(originalText, rewrittenText), [originalText, rewrittenText])

  if (!originalText.trim()) {
    return <div className="script-rewrite-diff-empty">原始稿为空，无法生成对比。</div>
  }
  if (!rewrittenText.trim()) {
    return <div className="script-rewrite-diff-empty">暂无改写结果，请先点击“预览改写”。</div>
  }

  if (mode === 'unified') {
    return (
      <div className="script-rewrite-diff-unified">
        {rows.map((row, idx) => {
          const prefix = row.kind === 'add' ? '+' : row.kind === 'remove' ? '-' : ' '
          const text = row.kind === 'add' ? row.rightText : row.leftText
          return (
            <div key={`${idx}-${row.kind}`} className={`script-rewrite-diff-row ${row.kind}`}>
              <span className="prefix">{prefix}</span>
              <span className="text">{text || ' '}</span>
            </div>
          )
        })}
      </div>
    )
  }

  return (
    <div className="script-rewrite-diff-split">
      <div className="column">
        <div className="column-title">原始稿</div>
        {rows.map((row, idx) => (
          <div key={`l-${idx}-${row.kind}`} className={`script-rewrite-diff-row ${row.kind}`}>
            <span className="text">{row.leftText || ' '}</span>
          </div>
        ))}
      </div>
      <div className="column">
        <div className="column-title">改写稿</div>
        {rows.map((row, idx) => (
          <div key={`r-${idx}-${row.kind}`} className={`script-rewrite-diff-row ${row.kind}`}>
            <span className="text">{row.rightText || ' '}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
