import { AppInput } from '@/components/common/AppInput'
import { AppButton } from '@/components/common/AppButton'
import { ScriptRewriteDiffPanel, type RewriteDiffMode } from '@/components/script/ScriptRewriteDiffPanel'

type Props = {
  visible: boolean
  instruction: string
  targetStyle: string
  maxOutputChars: string
  originalText: string
  previewText: string
  diffMode: RewriteDiffMode
  loading?: boolean
  applying?: boolean
  previewSubtitle?: string
  onChangeInstruction: (value: string) => void
  onChangeTargetStyle: (value: string) => void
  onChangeMaxOutputChars: (value: string) => void
  onChangeDiffMode: (mode: RewriteDiffMode) => void
  onCancel: () => void
  onPreview: () => void
  onApply: () => void
}

export function ScriptRewriteDialog({
  visible,
  instruction,
  targetStyle,
  maxOutputChars,
  originalText,
  previewText,
  diffMode,
  loading,
  applying,
  previewSubtitle,
  onChangeInstruction,
  onChangeTargetStyle,
  onChangeMaxOutputChars,
  onChangeDiffMode,
  onCancel,
  onPreview,
  onApply,
}: Props) {
  if (!visible) return null
  return (
    <div className="dialog-overlay" role="dialog" aria-modal onClick={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="dialog glass" style={{ maxWidth: 980 }}>
        <h3 className="dialog-title">AI 剧本改写</h3>
        <div style={{ display: 'grid', gap: 'var(--space-sm)' }}>
          <AppInput
            label="改写要求"
            as="textarea"
            rows={4}
            value={instruction}
            onChange={(v) => onChangeInstruction(String(v))}
            placeholder="例如：保持剧情主线不变，强化人物冲突与情绪递进，对白更自然。"
          />
          <AppInput
            label="目标风格（可选）"
            value={targetStyle}
            onChange={(v) => onChangeTargetStyle(String(v))}
            placeholder="例如：现实主义 + 悬疑张力"
          />
          <AppInput
            label="字数上限（可选）"
            value={maxOutputChars}
            onChange={(v) => onChangeMaxOutputChars(String(v))}
            placeholder="例如：4500"
          />
        </div>
        <div className="dialog-actions" style={{ marginTop: 'var(--space-md)' }}>
          <AppButton variant="ghost" loading={loading} onClick={onPreview}>
            预览改写
          </AppButton>
        </div>
        <div className="dialog-actions" style={{ marginTop: 'var(--space-sm)' }}>
          <button type="button" className={`btn-cancel ${diffMode === 'split' ? 'active' : ''}`} onClick={() => onChangeDiffMode('split')}>
            分栏对比
          </button>
          <button type="button" className={`btn-cancel ${diffMode === 'unified' ? 'active' : ''}`} onClick={() => onChangeDiffMode('unified')}>
            行内对比
          </button>
        </div>
        <div className="panel glass script-rewrite-diff-wrap">
          {previewSubtitle ? <p className="muted small">{previewSubtitle}</p> : null}
          <p className="muted small">对比基准：原始剧本</p>
          <ScriptRewriteDiffPanel originalText={originalText} rewrittenText={previewText} mode={diffMode} />
        </div>
        <div className="dialog-actions">
          <button type="button" className="btn-cancel" onClick={onCancel} disabled={!!loading || !!applying}>
            取消
          </button>
          <button type="button" className="btn-confirm" onClick={onApply} disabled={!!loading || !!applying || !previewText.trim()}>
            应用改写
          </button>
        </div>
      </div>
    </div>
  )
}
