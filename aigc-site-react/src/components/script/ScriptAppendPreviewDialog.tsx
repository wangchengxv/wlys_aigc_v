type Props = {
  visible: boolean
  title?: string
  subtitle?: string
  appendText: string
  loading?: boolean
  onCancel: () => void
  onConfirmAppend: () => void
}

export function ScriptAppendPreviewDialog({
  visible,
  title = 'AI 续写剧本预览',
  subtitle,
  appendText,
  loading,
  onCancel,
  onConfirmAppend,
}: Props) {
  if (!visible) return null
  return (
    <div className="dialog-overlay" role="dialog" aria-modal onClick={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="dialog glass" style={{ maxWidth: 920 }}>
        <h3 className="dialog-title">{title}</h3>
        {subtitle ? <p className="dialog-message">{subtitle}</p> : null}
        <div
          className="panel glass"
          style={{
            maxHeight: 420,
            overflow: 'auto',
            padding: 'var(--space-md)',
            whiteSpace: 'pre-wrap',
            lineHeight: 1.65,
          }}
        >
          {appendText || '（空）'}
        </div>
        <div className="dialog-actions">
          <button type="button" className="btn-cancel" onClick={onCancel} disabled={!!loading}>
            取消
          </button>
          <button type="button" className="btn-confirm" onClick={onConfirmAppend} disabled={!!loading || !appendText.trim()}>
            追加并保存
          </button>
        </div>
      </div>
    </div>
  )
}

