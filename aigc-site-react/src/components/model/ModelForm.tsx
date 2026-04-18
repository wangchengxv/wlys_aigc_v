import { useEffect, useState } from 'react'
import type { ConnectionConfig, ModelConfig, ModelConfigCreateRequest } from '@/types'

type Props = {
  visible: boolean
  editing?: ModelConfig | null
  connections: ConnectionConfig[]
  /** When creating a model, lock association to this connection (e.g. provider hub). */
  presetConnectionId?: string | null
  onClose: () => void
  onSubmit: (data: ModelConfigCreateRequest) => void
}

function normalize(value?: string | null) {
  return (value || '').trim().toLowerCase()
}

function inferCapabilities(provider?: string, modelName?: string): string[] {
  const caps: string[] = []
  const p = normalize(provider)
  const m = normalize(modelName)
  if (m.includes('seedream') || m.includes('image') || m.includes('flux') || m.includes('wanx') || m.includes('dall') || m.includes('sdxl')) {
    caps.push('image')
  }
  if (m.includes('seedance') || m.includes('video') || m.includes('veo') || m.includes('sora') || m.startsWith('vidu')) {
    caps.push('video')
  }
  if (m.startsWith('kling-')) {
    return caps
  }
  if (caps.length === 0 && p && p !== 'ark') {
    caps.push('text')
  }
  if (caps.length === 0 && p === 'ark') {
    caps.push('image')
  }
  return caps
}

export function ModelForm({ visible, editing, connections, presetConnectionId, onClose, onSubmit }: Props) {
  const [form, setForm] = useState<ModelConfigCreateRequest>({
    name: '',
    provider: '',
    modelName: '',
    connectionId: '',
    enabled: true,
    metadata: { capabilities: [] },
  })
  const [capabilities, setCapabilities] = useState<string[]>([])

  useEffect(() => {
    if (!visible) return
    if (editing) {
      const rawCaps = Array.isArray(editing.metadata?.capabilities)
        ? (editing.metadata.capabilities as string[])
        : inferCapabilities(editing.provider, editing.modelName)
      const caps = rawCaps.length ? [...rawCaps] : inferCapabilities(editing.provider, editing.modelName)
      setCapabilities(caps)
      setForm({
        name: editing.name,
        provider: editing.provider,
        modelName: editing.modelName,
        connectionId: editing.connectionId,
        enabled: editing.enabled,
        metadata: { ...(editing.metadata || {}), capabilities: caps },
      })
    } else {
      setCapabilities([])
      setForm({
        name: '',
        provider: '',
        modelName: '',
        connectionId: presetConnectionId ?? '',
        enabled: true,
        metadata: { capabilities: [] },
      })
    }
  }, [visible, editing, presetConnectionId])

  useEffect(() => {
    if (!visible || editing) return
    if (!presetConnectionId) return
    const conn = connections.find((c) => c.id === presetConnectionId)
    if (conn) {
      setForm((f) => ({ ...f, provider: conn.provider, connectionId: presetConnectionId }))
    }
  }, [visible, editing, presetConnectionId, connections])

  useEffect(() => {
    if (!visible) return
    if (editing?.id) return
    if (capabilities.length > 0) return
    setCapabilities(inferCapabilities(form.provider, form.modelName))
  }, [visible, editing?.id, form.provider, form.modelName, capabilities.length])

  if (!visible) return null

  function toggleCapability(capability: string) {
    setCapabilities((prev) => (prev.includes(capability) ? prev.filter((item) => item !== capability) : [...prev, capability]))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (capabilities.length === 0) return
    if (!form.name || !form.provider || !form.modelName || !form.connectionId) return
    const md = { ...(form.metadata || {}), capabilities }
    onSubmit({ ...form, metadata: md })
  }

  return (
    <div className="dialog-overlay" role="dialog" aria-modal onClick={(ev) => ev.target === ev.currentTarget && onClose()}>
      <div className="dialog glass modal-form-dialog">
        <h3 className="dialog-title">{editing ? '编辑模型' : '新建模型'}</h3>
        <form className="form" onSubmit={handleSubmit}>
          <label className="input-wrap">
            <span className="label">名称</span>
            <input
              className="ctrl"
              type="text"
              placeholder="例如：GPT-4o"
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </label>
          <label className="input-wrap">
            <span className="label">提供商</span>
            <input
              className="ctrl"
              type="text"
              placeholder="例如：OpenAI"
              required
              value={form.provider}
              onChange={(e) => setForm((f) => ({ ...f, provider: e.target.value }))}
            />
          </label>
          <label className="input-wrap">
            <span className="label">模型标识</span>
            <input
              className="ctrl"
              type="text"
              placeholder="例如：gpt-4o"
              required
              value={form.modelName}
              onChange={(e) => setForm((f) => ({ ...f, modelName: e.target.value }))}
            />
          </label>
          <label className="input-wrap">
            <span className="label">关联连接</span>
            <select
              className="ctrl"
              required
              disabled={!!presetConnectionId && !editing}
              value={form.connectionId}
              onChange={(e) => setForm((f) => ({ ...f, connectionId: e.target.value }))}
            >
              <option value="" disabled>
                请选择连接
              </option>
              {connections.map((conn) => (
                <option key={conn.id} value={conn.id}>
                  {conn.name} ({conn.provider})
                </option>
              ))}
            </select>
          </label>
          <label className="input-wrap">
            <span className="label">分组 (group)</span>
            <input
              className="ctrl"
              type="text"
              placeholder="列表分组用，可留空"
              value={String((form.metadata as Record<string, unknown>)?.group ?? '')}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  metadata: { ...(f.metadata || {}), group: e.target.value },
                }))
              }
            />
          </label>
          <label className="input-wrap">
            <span className="label">模型类型</span>
            <select
              className="ctrl"
              value={String((form.metadata as Record<string, unknown>)?.modelType ?? '')}
              onChange={(e) => {
                const v = e.target.value
                setForm((f) => {
                  const md = { ...(f.metadata || {}) } as Record<string, unknown>
                  if (v) md.modelType = v
                  else delete md.modelType
                  return { ...f, metadata: md }
                })
              }}
            >
              <option value="">未指定</option>
              <option value="chat">chat</option>
              <option value="embedding">embedding</option>
              <option value="image">image</option>
              <option value="video">video</option>
              <option value="rerank">rerank</option>
            </select>
          </label>
          <label className="input-wrap">
            <span className="label">备注</span>
            <input
              className="ctrl"
              type="text"
              placeholder="可选"
              value={String((form.metadata as Record<string, unknown>)?.notes ?? '')}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  metadata: { ...(f.metadata || {}), notes: e.target.value },
                }))
              }
            />
          </label>
          <label className="input-wrap toggle-wrap">
            <span className="label">启用状态</span>
            <input
              className="toggle"
              type="checkbox"
              checked={form.enabled}
              onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.checked }))}
            />
          </label>
          <div className="input-wrap">
            <span className="label">能力标签</span>
            {capabilities.length === 0 ? <p className="hint-error">请至少选择一个能力标签</p> : null}
            <div className="cap-list">
              {(['text', 'image', 'video', 'embedding', 'rerank'] as const).map((cap) => (
                <button
                  key={cap}
                  type="button"
                  className={`cap-btn${capabilities.includes(cap) ? ' active' : ''}`}
                  onClick={() => toggleCapability(cap)}
                >
                  {cap}
                </button>
              ))}
            </div>
          </div>
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
