type StatItem = {
  key: string
  label: string
  value: string | number
  hint?: string
}

type Props = {
  items: StatItem[]
  className?: string
  showHint?: boolean
}

export function StatStrip({ items, className = '', showHint = true }: Props) {
  return (
    <div className={['stat-strip', className].filter(Boolean).join(' ')}>
      {items.map((item) => (
        <article key={item.key} className="stat-strip__item">
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          {showHint && item.hint ? <small>{item.hint}</small> : null}
        </article>
      ))}
    </div>
  )
}
