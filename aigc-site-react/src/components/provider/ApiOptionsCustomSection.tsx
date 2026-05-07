import { useEffect, useState } from 'react'

type HeaderRow = { name: string; value: string }

function parseHeadersJson(raw: string): HeaderRow[] {
  const t = raw.trim()
  if (!t) return [{ name: '', value: '' }]
  try {
    const arr = JSON.parse(t) as unknown
    if (!Array.isArray(arr)) return [{ name: '', value: '' }]
    const rows: HeaderRow[] = []
    for (const item of arr) {
      if (item && typeof item === 'object' && 'name' in item) {
        const o = item as Record<string, unknown>
        rows.push({ name: String(o.name ?? ''), value: String(o.value ?? '') })
      }
    }
    return rows.length ? rows : [{ name: '', value: '' }]
  } catch {
    return [{ name: '', value: '' }]
  }
}

function serializeHeaders(rows: HeaderRow[]): string {
  const out = rows
    .filter((r) => r.name.trim() !== '')
    .map((r) => ({ name: r.name.trim(), value: r.value }))
  return out.length ? JSON.stringify(out) : ''
}

type Props = {
  meta: Record<string, string>
  onChange: (next: Record<string, string>) => void
}

/** Cherry-style API options: custom headers & extra query (stored in connection metadata). */
export function ApiOptionsCustomSection({ meta, onChange }: Props) {
  const [headerRows, setHeaderRows] = useState<HeaderRow[]>(() => parseHeadersJson(meta.customHeadersJson ?? ''))
  const [queryJson, setQueryJson] = useState(() => meta.customQueryParamsJson ?? '')

  useEffect(() => {
    setHeaderRows(parseHeadersJson(meta.customHeadersJson ?? ''))
    setQueryJson(meta.customQueryParamsJson ?? '')
  }, [meta.customHeadersJson, meta.customQueryParamsJson])

  function pushMeta(next: Record<string, string>) {
    onChange({ ...meta, ...next })
  }

  function updateHeaderRow(i: number, patch: Partial<HeaderRow>) {
    const next = headerRows.map((row, j) => (j === i ? { ...row, ...patch } : row))
    setHeaderRows(next)
    pushMeta({ customHeadersJson: serializeHeaders(next) })
  }

  function addHeaderRow() {
    const next = [...headerRows, { name: '', value: '' }]
    setHeaderRows(next)
    pushMeta({ customHeadersJson: serializeHeaders(next) })
  }

  function removeHeaderRow(i: number) {
    const next = headerRows.filter((_, j) => j !== i)
    const rows = next.length ? next : [{ name: '', value: '' }]
    setHeaderRows(rows)
    pushMeta({ customHeadersJson: serializeHeaders(rows) })
  }

  return (
    <div className="provider-api-options">
      <h4 className="form-section-title">API 选项与自定义请求</h4>
      <p className="catalog-hint muted">适用于 OpenAI 兼容网关；自定义 Header 在默认鉴权之后合并，可覆盖 Authorization。</p>
      <div className="custom-headers-editor">
        <span className="label">自定义 Header（JSON 数组）</span>
        {headerRows.map((row, i) => (
          <div key={i} className="custom-headers-editor__row">
            <input
              className="ctrl"
              type="text"
              placeholder="Header 名"
              value={row.name}
              onChange={(e) => updateHeaderRow(i, { name: e.target.value })}
            />
            <input
              className="ctrl"
              type="text"
              placeholder="值"
              value={row.value}
              onChange={(e) => updateHeaderRow(i, { value: e.target.value })}
            />
            <button type="button" className="btn-icon danger" onClick={() => removeHeaderRow(i)} aria-label="删除行">
              ×
            </button>
          </div>
        ))}
        <button type="button" className="btn-ghost link-as-button" onClick={addHeaderRow}>
          + 添加 Header
        </button>
      </div>
      <label className="input-wrap">
        <span className="label">额外 Query 参数（JSON 对象）</span>
        <textarea
          className="ctrl"
          rows={5}
          value={queryJson}
          onChange={(e) => {
            const v = e.target.value
            setQueryJson(v)
            pushMeta({ customQueryParamsJson: v })
          }}
          placeholder='例如 {"api_key":"..."}'
        />
      </label>
    </div>
  )
}
