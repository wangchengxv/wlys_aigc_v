import { useEffect, useMemo, useState } from 'react'
import { ApiOptionsCustomSection } from '@/components/provider/ApiOptionsCustomSection'
import { ExtraApiKeysSection } from '@/components/provider/ExtraApiKeysSection'
import { ProviderConnectionFields } from '@/components/provider/ProviderConnectionFields'
import type { ConnectionConfig, ConnectionConfigCreateRequest, ProviderCatalogEntry } from '@/types'

type Props = {
  visible: boolean
  editing?: ConnectionConfig | null
  /** When set, extra fields follow catalog gatewayKind. */
  catalog?: ProviderCatalogEntry[]
  onClose: () => void
  onSubmit: (data: ConnectionConfigCreateRequest) => void
}

function toMetaStrings(meta: Record<string, unknown> | undefined): Record<string, string> {
  if (!meta) return {}
  const out: Record<string, string> = {}
  for (const [k, v] of Object.entries(meta)) {
    if (k === 'capabilities') continue
    out[k] = v == null ? '' : String(v)
  }
  return out
}

export function ConnectionForm({ visible, editing, catalog, onClose, onSubmit }: Props) {
  const [form, setForm] = useState<ConnectionConfigCreateRequest>({
    name: '',
    provider: '',
    baseUrl: '',
    apiKey: '',
    enabled: true,
    metadata: {},
  })
  const [meta, setMeta] = useState<Record<string, string>>({})

  const catalogEntry = useMemo(() => {
    const p = form.provider.trim()
    if (!p || !catalog?.length) return null
    return catalog.find((c) => c.key === p) ?? null
  }, [catalog, form.provider])

  useEffect(() => {
    if (!visible) return
    if (editing) {
      setForm({
        name: editing.name,
        provider: editing.provider,
        baseUrl: editing.baseUrl,
        apiKey: '',
        enabled: editing.enabled,
        metadata: {},
      })
      setMeta(toMetaStrings(editing.metadata))
    } else {
      setForm({ name: '', provider: '', baseUrl: '', apiKey: '', enabled: true, metadata: {} })
      setMeta({})
    }
  }, [visible, editing])

  if (!visible) return null

  const ollamaLike = form.provider.trim().toLowerCase() === 'ollama'
  const lmLike = form.provider.trim().toLowerCase() === 'lm_studio'
  const gatewayKind = catalogEntry?.gatewayKind
  const isBedrock = gatewayKind === 'BEDROCK' || form.provider === 'aws_bedrock'
  const isVertex = gatewayKind === 'VERTEX' || form.provider === 'vertex_ai'
  const isAzure = gatewayKind === 'AZURE_OPENAI' || form.provider === 'azure_openai'
  const showHttpAdvanced = !isAzure && !isBedrock && !isVertex

  function buildMetadataPayload(): Record<string, unknown> {
    const out: Record<string, unknown> = {}
    for (const [k, v] of Object.entries(meta)) {
      if (v.trim() === '') continue
      out[k] = v
    }
    if (isAzure && !out.apiVersion) {
      out.apiVersion = '2024-06-01'
    }
    return out
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name || !form.provider || !form.baseUrl) return
    if (!editing && !ollamaLike && !lmLike && !isVertex && !form.apiKey.trim()) return
    if (isBedrock) {
      if (!meta.region?.trim() || !meta.awsAccessKeyId?.trim() || (!editing && !form.apiKey.trim())) return
    }
    if (isVertex) {
      if (!meta.vertexProjectId?.trim() || !meta.vertexLocation?.trim() || !meta.vertexServiceAccountJson?.trim()) return
    }
    const metadata = buildMetadataPayload()
    onSubmit({ ...form, metadata: Object.keys(metadata).length ? metadata : undefined })
  }

  const keyLabel = isBedrock ? 'Secret Access Key' : 'API Key'
  const keyRequired = !editing && !ollamaLike && !lmLike && !isVertex

  return (
    <div className="dialog-overlay" role="dialog" aria-modal onClick={(ev) => ev.target === ev.currentTarget && onClose()}>
      <div className="dialog glass modal-form-dialog">
        <h3 className="dialog-title">{editing ? '编辑连接' : '新建连接'}</h3>
        <form className="form" onSubmit={handleSubmit}>
          <h4 className="form-section-title">连接身份</h4>
          <label className="input-wrap">
            <span className="label">名称</span>
            <input
              className="ctrl"
              type="text"
              placeholder="例如：OpenAI API"
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </label>
          <label className="input-wrap">
            <span className="label">提供商 key</span>
            <input
              className="ctrl"
              type="text"
              placeholder="与目录一致，如 openai / azure_openai"
              required
              value={form.provider}
              onChange={(e) => setForm((f) => ({ ...f, provider: e.target.value }))}
            />
          </label>
          <label className="input-wrap">
            <span className="label">Base URL</span>
            <input
              className="ctrl"
              type="text"
              placeholder="https://… 或 http://localhost:11434"
              required
              value={form.baseUrl}
              onChange={(e) => setForm((f) => ({ ...f, baseUrl: e.target.value }))}
            />
          </label>

          <ProviderConnectionFields catalogEntry={catalogEntry} meta={meta} onChange={setMeta} />

          {!isVertex ? (
            <label className="input-wrap">
              <span className="label">{keyLabel}</span>
              <input
                className="ctrl"
                type="password"
                placeholder={editing ? '留空则不修改' : ollamaLike || lmLike ? '本地可留空' : '请输入密钥'}
                required={keyRequired}
                value={form.apiKey}
                onChange={(e) => setForm((f) => ({ ...f, apiKey: e.target.value }))}
              />
            </label>
          ) : null}

          {showHttpAdvanced ? (
            <>
              <ApiOptionsCustomSection meta={meta} onChange={setMeta} />
              <ExtraApiKeysSection meta={meta} onChange={setMeta} />
            </>
          ) : null}

          <label className="input-wrap toggle-wrap">
            <span className="label">启用状态</span>
            <input
              className="toggle"
              type="checkbox"
              checked={form.enabled}
              onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.checked }))}
            />
          </label>
          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn-submit">
              {editing ? '保存' : '创建'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
