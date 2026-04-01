import type { ProviderPanelProps } from './types'

export function VertexPanel({ meta, onChange }: ProviderPanelProps) {
  function setField(field: string, value: string) {
    onChange({ ...meta, [field]: value })
  }

  return (
    <div className="provider-extra-fields">
      <label className="input-wrap">
        <span className="label">GCP 项目 ID</span>
        <input
          className="ctrl"
          type="text"
          required
          value={meta.vertexProjectId ?? ''}
          onChange={(e) => setField('vertexProjectId', e.target.value)}
        />
      </label>
      <label className="input-wrap">
        <span className="label">区域 (location)</span>
        <input
          className="ctrl"
          type="text"
          required
          value={meta.vertexLocation ?? ''}
          onChange={(e) => setField('vertexLocation', e.target.value)}
          placeholder="us-central1"
        />
      </label>
      <label className="input-wrap">
        <span className="label">Service Account JSON</span>
        <textarea
          className="ctrl"
          rows={5}
          required
          autoComplete="off"
          value={meta.vertexServiceAccountJson ?? ''}
          onChange={(e) => setField('vertexServiceAccountJson', e.target.value)}
          placeholder="{...}"
        />
      </label>
      <p className="catalog-hint muted">凭据错误时上游会返回明确错误；请确认服务账号具备 Vertex AI 调用权限。</p>
    </div>
  )
}
