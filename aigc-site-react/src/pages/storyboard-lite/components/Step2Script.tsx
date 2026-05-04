import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import type { VideoModelOptionDetail } from '@/types'
import { ModelSelectorPanel, type ModelInputMode } from './ModelSelectorPanel'

export type { ModelInputMode } from './ModelSelectorPanel'

export interface Step2ScriptProps {
  imageModelInputMode: ModelInputMode
  setImageModelInputMode: (v: ModelInputMode) => void
  imageModel: string
  setImageModel: (v: string) => void
  customImageModel: string
  setCustomImageModel: (v: string) => void
  imageModelOptions: string[]
  imageModelDetails: VideoModelOptionDetail[]
  modelsLoading: boolean
  handleRefreshModels: () => Promise<void>
  scriptText: string
  setScriptText: (v: string) => void
  threeViewPrompt: string
  setThreeViewPrompt: (v: string) => void
  onSaveScript: () => Promise<void>
  onGenerateKeyframes: () => Promise<void>
  busy: boolean
  onNext: () => void
  hasKeyframes: boolean
}

export function Step2Script({
  imageModelInputMode,
  setImageModelInputMode,
  imageModel,
  setImageModel,
  customImageModel,
  setCustomImageModel,
  imageModelOptions,
  imageModelDetails,
  modelsLoading,
  handleRefreshModels,
  scriptText,
  setScriptText,
  threeViewPrompt,
  setThreeViewPrompt,
  onSaveScript,
  onGenerateKeyframes,
  busy,
  onNext,
  hasKeyframes,
}: Step2ScriptProps) {
  return (
    <div className="sl-step-content">
      <div className="sl-card">
        <h2 className="sl-card-title">第二步：剧本与三视图配置</h2>
        <p className="sl-card-desc">
          选择合适的图片模型并输入剧本内容，系统将自动生成三视图关键帧。
        </p>

        <div className="sl-form-grid">
          <ModelSelectorPanel
            variant="image"
            title="三视图图片模型"
            description="先选合适模型再生成关键帧，模型切换会实时生效。"
            mode={imageModelInputMode}
            onModeChange={setImageModelInputMode}
            selectedModel={imageModel}
            onSelectedModelChange={setImageModel}
            customModel={customImageModel}
            onCustomModelChange={setCustomImageModel}
            options={imageModelOptions}
            details={imageModelDetails}
            loading={modelsLoading}
            onRefresh={handleRefreshModels}
            refreshText="刷新模型列表"
            helperText="生成前可随时切换；选择会保存在本机，下次进入会自动带回。"
            customPlaceholder="例如：doubao-seedream-5.0-lite"
          />

          <AppInput
            as="textarea"
            rows={8}
            label="剧本内容"
            value={scriptText}
            onChange={(v) => setScriptText(String(v))}
            placeholder="请输入剧本..."
          />
          <AppInput
            as="textarea"
            rows={4}
            label="三视图提示词（文生图，可选）"
            value={threeViewPrompt}
            onChange={(v) => setThreeViewPrompt(String(v))}
            placeholder="不填则自动基于剧本生成提示词"
          />
        </div>

        <div className="sl-actions sl-actions-between">
          <AppButton loading={busy} onClick={() => void onSaveScript()}>
            保存剧本
          </AppButton>
          <div style={{ display: 'flex', gap: 'var(--space-md)' }}>
            <AppButton loading={busy} variant="primary" onClick={() => void onGenerateKeyframes()}>
              生成三视图
            </AppButton>
            {hasKeyframes && (
              <AppButton variant="ghost" onClick={onNext}>
                进入下一步
              </AppButton>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
