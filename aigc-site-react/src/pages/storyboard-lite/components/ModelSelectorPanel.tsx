import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import type { VideoModelOptionDetail } from '@/types'

export type ModelInputMode = 'preset' | 'custom'

type Props = {
  variant: 'image' | 'video'
  title: string
  description: string
  mode: ModelInputMode
  onModeChange: (value: ModelInputMode) => void
  selectedModel: string
  onSelectedModelChange: (value: string) => void
  customModel: string
  onCustomModelChange: (value: string) => void
  options: string[]
  details: VideoModelOptionDetail[]
  loading: boolean
  onRefresh?: () => Promise<void> | void
  refreshText?: string
  helperText?: string
  customPlaceholder?: string
}

function getModelMeta(modelName: string, details: VideoModelOptionDetail[]) {
  const detail = details.find((item) => item.modelName === modelName)
  const displayName = detail?.displayName?.trim()
  const provider = detail?.provider?.trim()
  return {
    displayName: displayName || modelName,
    provider: provider || '',
    modelName,
  }
}

function renderModelOptionLabel(modelName: string, details: VideoModelOptionDetail[]): string {
  const meta = getModelMeta(modelName, details)
  if (meta.provider) {
    return `${meta.displayName} · ${meta.provider}（${meta.modelName}）`
  }
  if (meta.displayName !== meta.modelName) {
    return `${meta.displayName}（${meta.modelName}）`
  }
  return meta.modelName
}

export function ModelSelectorPanel({
  variant,
  title,
  description,
  mode,
  onModeChange,
  selectedModel,
  onSelectedModelChange,
  customModel,
  onCustomModelChange,
  options,
  details,
  loading,
  onRefresh,
  refreshText = '刷新模型列表',
  helperText,
  customPlaceholder = '',
}: Props) {
  const currentModel = mode === 'custom' ? customModel.trim() : selectedModel.trim()
  const currentMeta = currentModel ? getModelMeta(currentModel, details) : null

  return (
    <section className={`sl-model-panel sl-model-panel--${variant}`} aria-label={title}>
      <div className="sl-model-panel__header">
        <h3 className="sl-model-panel__title">{title}</h3>
        <p className="sl-model-panel__desc">{description}</p>
      </div>

      <div className="sl-model-panel__modes" role="group" aria-label={`${title}输入方式`}>
        <button
          type="button"
          className={`sl-model-panel__mode-btn${mode === 'preset' ? ' is-active' : ''}`}
          onClick={() => onModeChange('preset')}
          aria-pressed={mode === 'preset'}
        >
          从可用模型选择
        </button>
        <button
          type="button"
          className={`sl-model-panel__mode-btn${mode === 'custom' ? ' is-active' : ''}`}
          onClick={() => onModeChange('custom')}
          aria-pressed={mode === 'custom'}
        >
          手动输入模型ID
        </button>
      </div>

      <div className="sl-model-panel__field">
        {mode === 'preset' ? (
          <label className="field sl-model-panel__select-wrap">
            <span className="label">当前模型</span>
            <select
              className="sl-model-panel__select"
              value={selectedModel}
              disabled={loading}
              onChange={(e) => onSelectedModelChange(e.target.value)}
            >
              {loading ? <option value="">加载中...</option> : null}
              {options.length === 0 && !loading ? <option value="">暂无可用模型</option> : null}
              {options.map((item) => (
                <option key={item} value={item}>
                  {renderModelOptionLabel(item, details)}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <AppInput
            label="当前模型"
            value={customModel}
            onChange={(value) => onCustomModelChange(String(value))}
            placeholder={customPlaceholder}
          />
        )}
      </div>

      <div className="sl-model-panel__footer">
        {onRefresh ? (
          <AppButton
            size="sm"
            variant="ghost"
            loading={loading}
            onClick={() => {
              void onRefresh()
            }}
          >
            {refreshText}
          </AppButton>
        ) : null}
        {helperText ? <span className="sl-model-panel__helper">{helperText}</span> : null}
      </div>

      {currentMeta ? (
        <div className="sl-model-panel__meta">
          <span className="sl-model-panel__meta-label">当前生效模型</span>
          <strong className="sl-model-panel__meta-name">{currentMeta.displayName}</strong>
          <span className="sl-model-panel__meta-id">{currentMeta.modelName}</span>
          <span className="sl-model-panel__meta-provider">{currentMeta.provider || '未标注 provider'}</span>
        </div>
      ) : null}
    </section>
  )
}
