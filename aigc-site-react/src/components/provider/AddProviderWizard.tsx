import { useEffect, useMemo, useState } from 'react'
import { createConnection } from '@/api'
import { ProviderConnectionFields } from '@/components/provider/ProviderConnectionFields'
import { useToast } from '@/context/ToastContext'
import type { ConnectionConfig, ProviderCatalogEntry } from '@/types'

type Props = {
  visible: boolean
  catalog: ProviderCatalogEntry[]
  /** 目录尚未从后端返回时为 true */
  catalogLoading?: boolean
  onClose: () => void
  onCreated: (created?: ConnectionConfig) => Promise<void> | void
}

export function AddProviderWizard({
  visible,
  catalog,
  catalogLoading = false,
  onClose,
  onCreated,
}: Props) {
  const { showToast } = useToast()
  const [catalogSearch, setCatalogSearch] = useState('')
  const [selectedKey, setSelectedKey] = useState('')
  const [name, setName] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [meta, setMeta] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)

  const filteredCatalog = useMemo(() => {
    const q = catalogSearch.trim().toLowerCase()
    if (!q) return catalog
    return catalog.filter(
      (c) =>
        c.key.toLowerCase().includes(q) ||
        c.displayName.toLowerCase().includes(q) ||
        c.apiFormat.toLowerCase().includes(q),
    )
  }, [catalog, catalogSearch])

  const selected = catalog.find((c) => c.key === selectedKey)

  useEffect(() => {
    if (!visible) return
    setCatalogSearch('')
    setSelectedKey('')
    setName('')
    setBaseUrl('')
    setApiKey('')
    setMeta({})
  }, [visible])

  useEffect(() => {
    if (!visible || !selectedKey) return
    const sel = catalog.find((c) => c.key === selectedKey)
    if (sel) {
      setBaseUrl(sel.defaultBaseUrl)
      setName(`${sel.displayName} 连接`)
    }
  }, [selectedKey, visible, catalog])

  useEffect(() => {
    if (!visible) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [visible, onClose])

  if (!visible) return null

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!selected) return
    if (!name.trim() || !baseUrl.trim()) return
    const isVertex = selected.gatewayKind === 'VERTEX' || selected.key === 'vertex_ai'
    if (selected.authMode !== 'NONE' && !isVertex && !apiKey.trim()) return
    if (selected.gatewayKind === 'BEDROCK' || selected.key === 'aws_bedrock') {
      if (!meta.region?.trim() || !meta.awsAccessKeyId?.trim() || !apiKey.trim()) {
        showToast('请填写 Region、Access Key ID 与 Secret Access Key', 'error')
        return
      }
    }
    if (isVertex) {
      if (!meta.vertexProjectId?.trim() || !meta.vertexLocation?.trim() || !meta.vertexServiceAccountJson?.trim()) {
        showToast('请填写 Vertex 项目、区域与 Service Account JSON', 'error')
        return
      }
    }
    setLoading(true)
    try {
      const metadata: Record<string, unknown> = {}
      for (const [k, v] of Object.entries(meta)) {
        if (v.trim()) metadata[k] = v
      }
      if ((selected.gatewayKind === 'AZURE_OPENAI' || selected.key === 'azure_openai') && !metadata.apiVersion) {
        metadata.apiVersion = '2024-06-01'
      }
      const created = await createConnection({
        name: name.trim(),
        provider: selected.key,
        baseUrl: baseUrl.trim(),
        apiKey: selected.authMode === 'NONE' || isVertex ? '' : apiKey,
        enabled: true,
        metadata: Object.keys(metadata).length ? metadata : undefined,
      })
      await onCreated(created)
      onClose()
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="dialog-overlay"
      role="dialog"
      aria-modal
      aria-labelledby="add-provider-wizard-title"
      onClick={(ev) => ev.target === ev.currentTarget && onClose()}
    >
      <div className="dialog glass modal-form-dialog add-provider-wizard">
        <div className="dialog-title-row">
          <h3 id="add-provider-wizard-title" className="dialog-title">
            添加服务商
          </h3>
          <button type="button" className="dialog-close-btn" aria-label="关闭" onClick={onClose}>
            ×
          </button>
        </div>
        <p className="dialog-message muted">从目录选择类型后填写接入信息；密钥仅保存在本机后端。按 Esc 关闭。</p>
        {catalogLoading && catalog.length === 0 ? (
          <p className="catalog-loading-hint muted">正在加载服务商目录…</p>
        ) : null}
        <form className="form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="input-wrap">
            <span className="label">筛选类型</span>
            <input
              className="ctrl"
              type="search"
              value={catalogSearch}
              onChange={(e) => setCatalogSearch(e.target.value)}
              placeholder="输入名称或 key 过滤…"
              aria-label="筛选服务商"
            />
          </label>
          <label className="input-wrap">
            <span className="label">服务商类型</span>
            <select
              className="ctrl"
              required
              value={selectedKey}
              onChange={(e) => setSelectedKey(e.target.value)}
            >
              <option value="" disabled>
                {filteredCatalog.length ? '请选择' : catalog.length ? '无匹配项，请调整筛选' : '暂无目录'}
              </option>
              {filteredCatalog.map((c) => (
                <option key={c.key} value={c.key}>
                  {c.displayName} ({c.key})
                </option>
              ))}
            </select>
          </label>
          {selected ? (
            <p className="catalog-hint muted">
              协议：{selected.apiFormat} · 网关：{selected.gatewayKind} · 文本代理：
              {selected.textProxySupported ? '支持' : '否'}
              {selected.imageProxySupported ? ' · 图片' : ''}
              {selected.videoProxySupported ? ' · 视频' : ''}
            </p>
          ) : null}
          {selected && selected.staticModels.length > 0 ? (
            <div className="static-models-hint muted">
              <span className="label">参考模型 ID：</span>
              <span className="static-models-chips">
                {selected.staticModels.slice(0, 6).map((m) => (
                  <code key={m}>{m}</code>
                ))}
                {selected.staticModels.length > 6 ? <span>…</span> : null}
              </span>
            </div>
          ) : null}
          <label className="input-wrap">
            <span className="label">连接名称</span>
            <input
              className="ctrl"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="例如：OpenAI 生产"
            />
          </label>
          <label className="input-wrap">
            <span className="label">Base URL</span>
            <input
              className="ctrl"
              type="text"
              required
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="https://… 或 http://localhost:11434"
            />
          </label>
          <ProviderConnectionFields catalogEntry={selected ?? undefined} meta={meta} onChange={setMeta} />

          {selected && selected.authMode !== 'NONE' && selected.gatewayKind !== 'VERTEX' && selected.key !== 'vertex_ai' ? (
            <label className="input-wrap">
              <span className="label">
                {selected.gatewayKind === 'BEDROCK' || selected.key === 'aws_bedrock' ? 'Secret Access Key' : 'API Key'}
              </span>
              <input
                className="ctrl"
                type="password"
                required
                autoComplete="off"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder={selected.authMode === 'X_API_KEY' ? 'Anthropic x-api-key' : 'Bearer / api-key'}
              />
            </label>
          ) : selected &&
            (selected.authMode === 'NONE' || selected.gatewayKind === 'VERTEX' || selected.key === 'vertex_ai') ? (
            <p className="catalog-hint muted">
              {selected.gatewayKind === 'VERTEX' || selected.key === 'vertex_ai'
                ? 'Vertex 使用 Service Account JSON 认证，无需在此填写 API Key。'
                : '当前类型无需 API Key（如本地 Ollama）。'}
            </p>
          ) : null}
          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn-submit" disabled={loading || !selectedKey || (catalogLoading && catalog.length === 0)}>
              {loading ? '创建中…' : '创建连接'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
