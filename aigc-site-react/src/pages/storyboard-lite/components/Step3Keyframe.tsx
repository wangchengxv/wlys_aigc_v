import { AppButton } from '@/components/common/AppButton'
import { resolveApiMediaUrl, resolveScriptFileUrl } from '@/api'
import type { StoryboardLiteKeyframe } from '@/types'

export interface Step3KeyframeProps {
  keyframes: StoryboardLiteKeyframe[]
  onConfirmKeyframe: (item: StoryboardLiteKeyframe) => Promise<void>
  busy: boolean
  onNext: () => void
  hasConfirmed: boolean
}

export function Step3Keyframe({
  keyframes,
  onConfirmKeyframe,
  busy,
  onNext,
  hasConfirmed,
}: Step3KeyframeProps) {
  return (
    <div className="sl-step-content">
      <div className="sl-card">
        <h2 className="sl-card-title">第三步：关键帧确认</h2>
        <p className="sl-card-desc">浏览生成的三视图关键帧，确认一张作为后续视频生成的首帧参考。</p>

        {keyframes.length > 0 ? (
          <div className="sl-assets-grid">
            {keyframes.map((item) => {
              const src = resolveApiMediaUrl(item.imageUrl || resolveScriptFileUrl(item.imageFileId))
              return (
                <article
                  key={item.keyframeId}
                  className={`sl-asset-card ${item.selected ? 'selected' : ''}`}
                >
                  <div className="sl-asset-image-wrap">
                    {src ? (
                      <img className="sl-asset-image" src={src} alt={item.keyframeId} />
                    ) : (
                      <p className="muted" style={{ padding: 'var(--space-md)' }}>
                        无预览
                      </p>
                    )}
                  </div>
                  <div className="sl-asset-content">
                    <p className="sl-asset-prompt" title={item.promptText || '关键帧提示词'}>
                      {item.promptText || '关键帧提示词'}
                    </p>
                    <div className="sl-asset-meta">
                      <span>{item.modelName ? `模型：${item.modelName}` : '未知模型'}</span>
                    </div>
                    <AppButton
                      size="sm"
                      variant={item.selected ? 'primary' : 'ghost'}
                      loading={busy}
                      onClick={() => void onConfirmKeyframe(item)}
                      style={{ marginTop: 'var(--space-md)' }}
                    >
                      {item.selected ? '已确认此帧' : '确认此关键帧'}
                    </AppButton>
                  </div>
                </article>
              )
            })}
          </div>
        ) : (
          <p className="muted" style={{ textAlign: 'center', padding: 'var(--space-2xl)' }}>
            尚未生成关键帧，请返回上一步生成。
          </p>
        )}

        <div className="sl-actions sl-actions-between">
          <div />
          {hasConfirmed && (
            <AppButton variant="primary" onClick={onNext}>
              进入视频生成
            </AppButton>
          )}
        </div>
      </div>
    </div>
  )
}
