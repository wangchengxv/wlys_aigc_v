import type { ProviderPanelProps } from './types'

/** LM Studio / Ollama: no extra fields, hint only. */
export function LocalCompatPanel({ catalogEntry }: ProviderPanelProps) {
  const key = catalogEntry?.key ?? ''
  return (
    <p className="catalog-hint muted">
      {key === 'ollama' ? '本地 Ollama 通常无需密钥。' : 'LM Studio 本地服务默认无密钥；模型列表可通过连接测试拉取。'}
    </p>
  )
}
