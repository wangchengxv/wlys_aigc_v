import type { ReactNode } from 'react'
import { HelpHint } from '@/components/common/HelpHint'
import { StatStrip } from '@/components/common/StatStrip'
import { ScriptProjectWorkflowNav } from '@/components/script/ScriptProjectWorkflowNav'

type StatItem = {
  key: string
  label: string
  value: string | number
  hint?: string
}

type Props = {
  projectId: string
  title: string
  description: string
  meta?: ReactNode
  toolbar?: ReactNode
  stats?: StatItem[]
  helpTitle?: string
  help?: ReactNode
  className?: string
  children: ReactNode
}

export function ProjectSubpageShell({
  projectId,
  title,
  description,
  meta,
  toolbar,
  stats,
  helpTitle,
  help,
  className = '',
  children,
}: Props) {
  return (
    <div className="script-project-workflow-layout">
      <ScriptProjectWorkflowNav projectId={projectId} />
      <div className="script-project-workflow-layout__main">
        <section className={['project-subpage-shell', className].filter(Boolean).join(' ')}>
          <header className="project-subpage-shell__hero panel glass">
            <div className="project-subpage-shell__copy">
              <h2>{title}</h2>
              <p className="eyebrow">项目工作区</p>
              <p className="muted">{description}</p>
            </div>
            {meta ? <div className="project-subpage-shell__meta">{meta}</div> : null}
          </header>

          {stats?.length ? <StatStrip items={stats} className="project-subpage-shell__stats" /> : null}

          {helpTitle && help ? (
            <HelpHint title={helpTitle} className="project-subpage-shell__help">
              {help}
            </HelpHint>
          ) : null}

          {toolbar ? <section className="project-subpage-shell__toolbar panel glass">{toolbar}</section> : null}

          <div className="project-subpage-shell__content">{children}</div>

        </section>
      </div>
    </div>
  )
}
