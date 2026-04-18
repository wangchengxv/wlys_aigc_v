import { useMemo, useState } from 'react'

type Props = {
  artDirectionJson?: string | null
  onCopy?: (label: string) => void
}

type JsonRecord = Record<string, unknown>

function safeParseJson(text: string | null | undefined): JsonRecord | null {
  if (!text?.trim()) return null
  try {
    return JSON.parse(text) as JsonRecord
  } catch {
    return null
  }
}

async function copyToClipboard(text: string) {
  await navigator.clipboard.writeText(text)
}

function toText(value: unknown): string {
  if (value == null) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  if (Array.isArray(value)) return value.map((v) => toText(v)).filter(Boolean).join(', ')
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function extractColorTokens(text: string): string[] {
  const tokens: string[] = []
  const hexMatches = text.match(/#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})\b/g) ?? []
  tokens.push(...hexMatches)
  const rgbMatches = text.match(/rgba?\([^)]+\)/g) ?? []
  tokens.push(...rgbMatches)
  const hslMatches = text.match(/hsla?\([^)]+\)/g) ?? []
  tokens.push(...hslMatches)
  return Array.from(new Set(tokens))
}

function Section({
  title,
  content,
  allowSwatches,
  onCopy,
}: {
  title: string
  content: unknown
  allowSwatches?: boolean
  onCopy?: (label: string) => void
}) {
  const text = toText(content)
  const swatches = useMemo(() => (allowSwatches ? extractColorTokens(text) : []), [allowSwatches, text])

  async function handleCopy(label: string, value: string) {
    try {
      await copyToClipboard(value)
      onCopy?.(label)
    } catch {
      /* ignore */
    }
  }

  return (
    <section className="artd-card">
      <div className="artd-card-head">
        <strong>{title}</strong>
        <button type="button" className="link-btn" onClick={() => void handleCopy(title, text)}>
          复制分区
        </button>
      </div>
      {swatches.length ? (
        <div className="artd-swatches" aria-label={`${title} 色板`}>
          {swatches.map((c) => (
            <button
              key={c}
              type="button"
              className="artd-swatch"
              style={{ background: c }}
              onClick={() => void handleCopy(`${title} 色值`, c)}
            >
              <span className="artd-swatch-label">{c}</span>
            </button>
          ))}
        </div>
      ) : null}
      {Array.isArray(content) ? (
        <ul className="artd-list">
          {content.map((item, idx) => {
            const t = toText(item)
            return (
              <li key={idx} className="artd-list-item">
                <span className="muted">{t}</span>
                <button type="button" className="link-btn" onClick={() => void handleCopy(`${title} #${idx + 1}`, t)}>
                  复制
                </button>
              </li>
            )
          })}
        </ul>
      ) : typeof content === 'object' && content != null ? (
        <div className="artd-kv">
          {Object.entries(content as JsonRecord).map(([k, v]) => {
            const vText = toText(v)
            return (
              <div key={k} className="artd-kv-row">
                <div className="artd-kv-main">
                  <span className="artd-kv-key">{k}</span>
                  <span className="artd-kv-val muted">{vText}</span>
                </div>
                <button type="button" className="link-btn" onClick={() => void handleCopy(`${title}.${k}`, vText)}>
                  复制
                </button>
              </div>
            )
          })}
        </div>
      ) : (
        <p className="muted artd-paragraph">{text}</p>
      )}
    </section>
  )
}

export function ArtDirectionPreview({ artDirectionJson, onCopy }: Props) {
  const parsed = useMemo(() => safeParseJson(artDirectionJson), [artDirectionJson])
  const [showRaw, setShowRaw] = useState(false)

  const consistencyAnchors = useMemo(() => {
    const v = parsed?.consistencyAnchors
    return typeof v === 'string' ? v : ''
  }, [parsed])

  if (!artDirectionJson?.trim()) {
    return <p className="muted">尚未生成美术指导，请点击上方「生成美术指导 B-1」。</p>
  }

  if (!parsed) {
    return (
      <div className="artd-invalid">
        <p className="muted">美术指导 JSON 解析失败（可能含有非 JSON 文本）。你仍可复制原文用于排查。</p>
        <div className="artd-raw-actions">
          <button
            type="button"
            className="pill small"
            onClick={() => {
              void (async () => {
                try {
                  await copyToClipboard(artDirectionJson)
                  onCopy?.('原始 JSON')
                } catch {
                  /* ignore */
                }
              })()
            }}
          >
            复制原文
          </button>
        </div>
        <pre className="art-pre">{artDirectionJson}</pre>
      </div>
    )
  }

  const colorPalette = parsed.colorPalette
  const characterDesignRules = parsed.characterDesignRules
  const lightingStyle = parsed.lightingStyle
  const textureStyle = parsed.textureStyle
  const moodKeywords = parsed.moodKeywords

  return (
    <div className="artd-wrap">
      <div className="artd-top">
        <div className="artd-top-left">
          <p className="eyebrow">Anchors</p>
          <p className="artd-anchors">{consistencyAnchors || '（无）'}</p>
        </div>
        <div className="artd-top-actions">
          <button
            type="button"
            className="pill small"
            onClick={() => {
              void (async () => {
                try {
                  await copyToClipboard(consistencyAnchors || '')
                  onCopy?.('consistencyAnchors')
                } catch {
                  /* ignore */
                }
              })()
            }}
          >
            复制 Anchors
          </button>
          <button type="button" className="pill small" onClick={() => setShowRaw((v) => !v)}>
            {showRaw ? '隐藏原始 JSON' : '查看原始 JSON'}
          </button>
        </div>
      </div>

      <div className="artd-grid">
        <Section title="colorPalette" content={colorPalette} allowSwatches onCopy={onCopy} />
        <Section title="characterDesignRules" content={characterDesignRules} onCopy={onCopy} />
        <Section title="lightingStyle" content={lightingStyle} onCopy={onCopy} />
        <Section title="textureStyle" content={textureStyle} onCopy={onCopy} />
        <Section title="moodKeywords" content={moodKeywords} onCopy={onCopy} />
      </div>

      {showRaw ? <pre className="art-pre">{JSON.stringify(parsed, null, 2)}</pre> : null}
    </div>
  )
}
