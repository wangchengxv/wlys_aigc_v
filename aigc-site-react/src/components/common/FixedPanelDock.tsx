import type { ReactNode } from 'react'
import { SectionTabs } from '@/components/common/SectionTabs'

export type FixedPanelDockItem = {
  id: string
  label: string
  eyebrow?: string
  summary?: string
  badge?: string
  content: ReactNode
}

type Props = {
  title: string
  description?: string
  items: FixedPanelDockItem[]
  activeId: string
  onChange: (id: string) => void
  className?: string
}

export function FixedPanelDock({ title, description, items, activeId, onChange, className = '' }: Props) {
  const activeItem = items.find((item) => item.id === activeId) ?? items[0]

  if (!activeItem) return null

  return (
    <section className={['fixed-panel-dock', className].filter(Boolean).join(' ')}>
      <div className="fixed-panel-dock__head">
        <div>
          <h3>{title}</h3>
          <p className="fixed-panel-dock__eyebrow">工作台面板</p>
          {description ? <p className="fixed-panel-dock__description">{description}</p> : null}
        </div>
        {activeItem.badge ? <span className="fixed-panel-dock__panel-badge">{activeItem.badge}</span> : null}
      </div>

      <SectionTabs
        className="fixed-panel-dock__tabs"
        items={items.map((item) => ({ id: item.id, label: item.label, badge: item.badge }))}
        activeId={activeItem.id}
        onChange={onChange}
      />

      <div className="fixed-panel-dock__panel" role="tabpanel">
        <div className="fixed-panel-dock__panel-head">
          <div>
            <h3>{activeItem.label}</h3>
            {activeItem.eyebrow ? <p className="fixed-panel-dock__eyebrow">{activeItem.eyebrow}</p> : null}
          </div>
        </div>
        {activeItem.summary ? <p className="fixed-panel-dock__panel-summary">{activeItem.summary}</p> : null}
        <div className="fixed-panel-dock__panel-body">{activeItem.content}</div>
      </div>
    </section>
  )
}
