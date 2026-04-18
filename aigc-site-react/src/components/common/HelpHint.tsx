import type { ReactNode } from 'react'

type Props = {
  title: string
  children: ReactNode
  className?: string
}

export function HelpHint({ title, children, className = '' }: Props) {
  return (
    <details className={['help-hint', className].filter(Boolean).join(' ')}>
      <summary>{title}</summary>
      <div className="help-hint__body">{children}</div>
    </details>
  )
}
