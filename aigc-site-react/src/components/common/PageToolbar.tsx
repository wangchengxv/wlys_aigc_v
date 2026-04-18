import type { ReactNode } from 'react'

type Props = {
  eyebrow?: string
  title: string
  description?: string
  meta?: ReactNode
  actions?: ReactNode
  className?: string
}

export function PageToolbar({ eyebrow, title, description, meta, actions, className = '' }: Props) {
  return (
    <header className={['page-toolbar', className].filter(Boolean).join(' ')}>
      <div className="page-toolbar__copy">
        <div>
          <h1>{title}</h1>
          {eyebrow ? <p className="page-toolbar__eyebrow">{eyebrow}</p> : null}
          {description ? <p className="page-toolbar__description">{description}</p> : null}
        </div>
        {meta ? <div className="page-toolbar__meta">{meta}</div> : null}
      </div>
      {actions ? <div className="page-toolbar__actions">{actions}</div> : null}
    </header>
  )
}
