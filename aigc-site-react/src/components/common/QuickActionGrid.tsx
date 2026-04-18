import { Link } from 'react-router-dom'

type QuickActionItem = {
  key: string
  title: string
  description?: string
  to: string
  badge?: string
}

type Props = {
  items: QuickActionItem[]
  className?: string
}

export function QuickActionGrid({ items, className = '' }: Props) {
  return (
    <div className={['quick-action-grid', className].filter(Boolean).join(' ')}>
      {items.map((item) => (
        <Link key={item.key} to={item.to} className="quick-action-grid__item">
          <div>
            <strong>{item.title}</strong>
            {item.description ? <span>{item.description}</span> : null}
          </div>
          {item.badge ? <em>{item.badge}</em> : null}
        </Link>
      ))}
    </div>
  )
}
