import type { ProviderPanelProps } from './types'

export function BedrockPanel({ meta, onChange }: ProviderPanelProps) {
  function setField(field: string, value: string) {
    onChange({ ...meta, [field]: value })
  }

  return (
    <div className="provider-extra-fields">
      <label className="input-wrap">
        <span className="label">AWS Region</span>
        <input
          className="ctrl"
          type="text"
          required
          value={meta.region ?? ''}
          onChange={(e) => setField('region', e.target.value)}
          placeholder="us-east-1"
        />
      </label>
      <label className="input-wrap">
        <span className="label">Access Key ID</span>
        <input
          className="ctrl"
          type="text"
          required
          autoComplete="off"
          value={meta.awsAccessKeyId ?? ''}
          onChange={(e) => setField('awsAccessKeyId', e.target.value)}
        />
      </label>
      <label className="input-wrap">
        <span className="label">Session Token（可选）</span>
        <input
          className="ctrl"
          type="password"
          autoComplete="off"
          value={meta.awsSessionToken ?? ''}
          onChange={(e) => setField('awsSessionToken', e.target.value)}
          placeholder="临时凭证时填写"
        />
      </label>
      <p className="catalog-hint muted">Secret Access Key 请填在下方「密钥」字段。</p>
    </div>
  )
}
