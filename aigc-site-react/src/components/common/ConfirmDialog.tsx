type Props = {
  visible: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  visible,
  title,
  message,
  confirmText = '确认',
  cancelText = '取消',
  onConfirm,
  onCancel,
}: Props) {
  if (!visible) return null
  return (
    <div className="dialog-overlay" role="dialog" aria-modal onClick={(e) => e.target === e.currentTarget && onCancel()}>
      <div className="dialog glass">
        <h3 className="dialog-title">{title}</h3>
        <p className="dialog-message">{message}</p>
        <div className="dialog-actions">
          <button type="button" className="btn-cancel" onClick={onCancel}>
            {cancelText}
          </button>
          <button type="button" className="btn-confirm" onClick={onConfirm}>
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}
