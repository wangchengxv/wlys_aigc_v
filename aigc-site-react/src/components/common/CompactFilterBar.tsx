import type { ReactNode } from 'react'

type Props = {
  title?: string
  summary?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
}

export function CompactFilterBar({ title, summary, actions, children, className = '' }: Props) {
  return (
    <section className={['compact-filter-bar', className].filter(Boolean).join(' ')}>
      {(title || summary || actions) ? (
        <div className="compact-filter-bar__head">
          <div>
            {title ? <h3>{title}</h3> : null}
            {summary ? <div className="compact-filter-bar__summary">{summary}</div> : null}
          </div>
          {actions ? <div className="compact-filter-bar__actions">{actions}</div> : null}
        </div>
      ) : null}
      <div className="compact-filter-bar__body">{children}</div>
    </section>
  )
}
