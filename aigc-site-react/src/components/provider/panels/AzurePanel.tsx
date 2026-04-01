import type { ProviderPanelProps } from './types'

export function AzurePanel({ meta, onChange }: ProviderPanelProps) {
  function setField(field: string, value: string) {
    onChange({ ...meta, [field]: value })
  }

  return (
    <div className="provider-extra-fields">
      <label className="input-wrap">
        <span className="label">API 版本 (apiVersion)</span>
        <input
          className="ctrl"
          type="text"
          value={meta.apiVersion ?? '2024-06-01'}
          onChange={(e) => setField('apiVersion', e.target.value)}
          placeholder="2024-06-01"
        />
      </label>
      <label className="input-wrap">
        <span className="label">默认部署名说明</span>
        <input
          className="ctrl"
          type="text"
          value={meta.azureDeploymentHint ?? ''}
          onChange={(e) => setField('azureDeploymentHint', e.target.value)}
          placeholder="可选：与控制台部署名一致的提示"
        />
      </label>
      <p className="catalog-hint muted">
        模型标识（model）需与 Azure 部署名一致；Base URL 为资源地址（如 https://xxx.openai.azure.com）。
      </p>
    </div>
  )
}
