import { useEffect, useMemo, useState } from 'react'
import { getPresetModels, quickCreateConnection } from '@/api'
import { useToast } from '@/context/ToastContext'
import type { PresetModelDto, QuickConnectionRequest } from '@/types'

type Props = {
  visible: boolean
  onClose: () => void
  onSuccess: () => void
  showOnelinkPromo?: boolean
  /** 跳转服务商中心（如 Cherry「其他服务商」） */
  onOtherProvider?: () => void
  /** 切换到高级模式连接配置 */
  onAdvancedMode?: () => void
}

export function QuickModelForm({ visible, onClose, onSuccess, showOnelinkPromo = false, onOtherProvider, onAdvancedMode }: Props) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [presetData, setPresetData] = useState<{ models: PresetModelDto[]; providers: string[] }>({ models: [], providers: [] })
  const [selectedProvider, setSelectedProvider] = useState('')
  const [selectedModel, setSelectedModel] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [unknownModel, setUnknownModel] = useState(false)

  const providerOptions = useMemo(() => {
    const set = new Set<string>()
    for (const p of presetData.providers) {
      if (p?.trim()) set.add(p.trim())
    }
    for (const m of presetData.models) {
      if (m.provider?.trim()) set.add(m.provider.trim())
    }
    return Array.from(set)
  }, [presetData.models, presetData.providers])

  const modelOptions = useMemo(() => {
    if (!selectedProvider) return []
    const selected = selectedProvider.trim().toLowerCase()
    return presetData.models.filter((m) => m.provider.trim().toLowerCase() === selected)
  }, [presetData.models, selectedProvider])

  useEffect(() => {
    if (!visible) return
    setSelectedProvider('')
    setSelectedModel('')
    setApiKey('')
    setUnknownModel(false)
    void (async () => {
      try {
        const data = await getPresetModels()
        setPresetData(data)
      } catch (e) {
        showToast(e instanceof Error ? e.message : '加载预置模型失败', 'error')
      }
    })()
  }, [visible, showToast])

  useEffect(() => {
    if (!selectedModel || !selectedProvider) {
      setUnknownModel(false)
      return
    }
    const selected = selectedProvider.trim().toLowerCase()
    const ok = presetData.models.some((m) => m.modelName === selectedModel && m.provider.trim().toLowerCase() === selected)
    setUnknownModel(!ok)
  }, [selectedModel, selectedProvider, presetData.models])

  if (!visible) return null

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!selectedProvider || !selectedModel || !apiKey) return
    setLoading(true)
    try {
      const req: QuickConnectionRequest = {
        provider: selectedProvider,
        modelName: selectedModel,
        apiKey,
        enabled: true,
      }
      await quickCreateConnection(req)
      showToast('连接与模型已创建', 'success')
      onSuccess()
      onClose()
    } catch (e) {
      showToast(e instanceof Error ? e.message : '创建失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="dialog-overlay" role="dialog" aria-modal onClick={(ev) => ev.target === ev.currentTarget && onClose()}>
      <div className="dialog glass modal-form-dialog quick-form">
        <h3 className="dialog-title">快捷配置</h3>
        <p className="dialog-message" style={{ marginBottom: 'var(--space-lg)' }}>
          选择模型并输入 API Key，即可快速完成配置
        </p>
        {showOnelinkPromo ? (
          <section className="quick-form-promo" aria-label="onelinkai-api 推荐">
            <p className="quick-form-promo__title">推荐购买 API：onelinkai-api</p>
            <a className="quick-form-promo__link" href="https://www.onelinkai.cloud" target="_blank" rel="noopener noreferrer">
              https://www.onelinkai.cloud
            </a>
            <img className="quick-form-promo__image" src="/recommend/onelinkai-login.png" alt="OneLink AI API 推荐" />
          </section>
        ) : null}
        <form className="form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="input-wrap">
            <span className="label">提供商</span>
            <select className="ctrl" required value={selectedProvider} onChange={(e) => setSelectedProvider(e.target.value)}>
              <option value="" disabled>
                请选择提供商
              </option>
              {providerOptions.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </label>
          <label className="input-wrap">
            <span className="label">模型</span>
            <select
              className="ctrl"
              required
              disabled={!selectedProvider}
              value={selectedModel}
              onChange={(e) => setSelectedModel(e.target.value)}
            >
              <option value="" disabled>
                {selectedProvider ? '请选择模型' : '请先选择提供商'}
              </option>
              {modelOptions.map((m) => (
                <option key={m.modelName} value={m.modelName}>
                  {m.displayName} ({m.modelName})
                </option>
              ))}
            </select>
          </label>
          {unknownModel ? (
            <div className="unknown-hint">
              该模型不在预置库中，请切换到
              <button
                type="button"
                className="link-btn"
                onClick={() => {
                  onAdvancedMode?.()
                }}
              >
                高级模式
              </button>
              手动配置
            </div>
          ) : null}
          <label className="input-wrap">
            <span className="label">API Key</span>
            <input className="ctrl" type="password" placeholder="请输入 API Key" required value={apiKey} onChange={(e) => setApiKey(e.target.value)} />
          </label>
          <div className="quick-form-or" role="separator">
            <span>或</span>
          </div>
          <button
            type="button"
            className="btn-quick-other-provider"
            onClick={() => {
              onOtherProvider?.()
            }}
          >
            选择其他服务商
          </button>
          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn-submit" disabled={loading || unknownModel || !selectedProvider || !selectedModel || !apiKey}>
              {loading ? '创建中...' : '创建'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
