type SectionTab = {
  id: string
  label: string
  badge?: string
}

type Props = {
  items: SectionTab[]
  activeId: string
  onChange: (id: string) => void
  className?: string
}

export function SectionTabs({ items, activeId, onChange, className = '' }: Props) {
  return (
    <div className={['section-tabs', className].filter(Boolean).join(' ')} role="tablist">
      {items.map((item) => {
        const active = item.id === activeId
        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={active}
            className={`section-tabs__tab${active ? ' is-active' : ''}`}
            onClick={() => onChange(item.id)}
          >
            <span>{item.label}</span>
            {item.badge ? <em>{item.badge}</em> : null}
          </button>
        )
      })}
    </div>
  )
}
