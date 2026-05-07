import type { ReactNode } from 'react'

type Props = {
  title: string
  description?: string
  children?: ReactNode
}

export function EmptyState({ title, description = '', children }: Props) {
  return (
    <div className="empty-state panel glass">
      <div className="orb" />
      <h3>{title}</h3>
      {description ? <p className="muted">{description}</p> : null}
      {children}
    </div>
  )
}
