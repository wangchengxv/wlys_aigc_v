type Props = {
  visible: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  confirmLoading?: boolean
  disableCancel?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  visible,
  title,
  message,
  confirmText = '确认',
  cancelText = '取消',
  confirmLoading = false,
  disableCancel = false,
  onConfirm,
  onCancel,
}: Props) {
  if (!visible) return null
  const cancelDisabled = disableCancel || confirmLoading
  return (
    <div
      className="dialog-overlay"
      role="dialog"
      aria-modal
      onClick={(e) => e.target === e.currentTarget && !cancelDisabled && onCancel()}
    >
      <div className="dialog glass">
        <h3 className="dialog-title">{title}</h3>
        <p className="dialog-message">{message}</p>
        <div className="dialog-actions">
          <button type="button" className="btn-cancel" disabled={cancelDisabled} onClick={onCancel}>
            {cancelText}
          </button>
          <button type="button" className="btn-confirm" disabled={confirmLoading} onClick={onConfirm}>
            {confirmLoading ? '处理中...' : confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}
