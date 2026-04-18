import type { ReactNode } from 'react'

type Props = {
  open: boolean
  title: string
  description?: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
}

export function ActionDrawer({ open, title, description, onClose, children, footer }: Props) {
  if (!open) return null

  return (
    <div className="action-drawer" role="dialog" aria-modal="true" aria-label={title}>
      <button type="button" className="action-drawer__backdrop" aria-label="关闭抽屉" onClick={onClose} />
      <div className="action-drawer__panel">
        <div className="action-drawer__head">
          <div>
            <h3>{title}</h3>
            {description ? <p>{description}</p> : null}
          </div>
          <button type="button" className="action-drawer__close" onClick={onClose}>
            关闭
          </button>
        </div>
        <div className="action-drawer__body">{children}</div>
        {footer ? <div className="action-drawer__footer">{footer}</div> : null}
      </div>
    </div>
  )
}
