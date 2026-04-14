import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ScriptProjectCard } from '@/components/script/ScriptProjectCard'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'

export type WorkflowHubStep = { title: string; body: string }

export type WorkflowHubHeaderAction = {
  label: string
  to: string
  variant?: 'primary' | 'default'
}

type Props = {
  eyebrow: string
  title: string
  lede: string
  headerActions: WorkflowHubHeaderAction[]
  steps: WorkflowHubStep[]
  projectsTitle: string
  projectsHint: string
  primaryCta: { label: string; to: (projectId: string) => string }
  secondaryCta?: { label: string; to: (projectId: string) => string }
  emptyState: { title: string; description: string; newProjectLabel?: string }
}

export function WorkflowScriptProjectHub({
  eyebrow,
  title,
  lede,
  headerActions,
  steps,
  projectsTitle,
  projectsHint,
  primaryCta,
  secondaryCta,
  emptyState,
}: Props) {
  const loadProjects = useScriptProjectStore((s) => s.loadProjects)
  const listLoading = useScriptProjectStore((s) => s.listLoading)
  const projects = useScriptProjectStore((s) => s.projects)

  useEffect(() => {
    void loadProjects({ deleted: false })
  }, [loadProjects])

  const newLabel = emptyState.newProjectLabel ?? '新建工程'

  return (
    <section className="workflow-sub-page workflow-sub-page--scenes">
      <header className="workflow-sub-page__head workflow-sub-page__head--split panel glass">
        <div className="workflow-sub-page__intro">
          <p className="eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          <p className="muted workflow-sub-page__lede">{lede}</p>
        </div>
        <div className="workflow-sub-page__actions workflow-sub-page__actions--head">
          {headerActions.map((a) => (
            <Link
              key={`${a.to}-${a.label}`}
              className={a.variant === 'primary' ? 'nav-btn primary' : 'nav-btn'}
              to={a.to}
            >
              {a.label}
            </Link>
          ))}
        </div>
      </header>

      <ol className="workflow-sub-page__steps workflow-sub-page__steps--timeline panel glass">
        {steps.map((step, i) => (
          <li key={step.title} className="workflow-sub-page__step">
            <span className="workflow-sub-page__step-index" aria-hidden>
              {i + 1}
            </span>
            <div className="workflow-sub-page__step-body">
              <p className="workflow-sub-page__step-title">{step.title}</p>
              <p className="muted workflow-sub-page__step-text">{step.body}</p>
            </div>
          </li>
        ))}
      </ol>

      <div className="workflow-sub-page__projects panel glass">
        <div className="workflow-sub-page__projects-bar">
          <h3 className="workflow-sub-page__projects-title">{projectsTitle}</h3>
          <p className="muted workflow-sub-page__projects-hint">{projectsHint}</p>
        </div>
        {listLoading ? (
          <LoadingSpinner />
        ) : projects.length ? (
          <div className="workflow-sub-page__grid">
            {projects.map((item) => (
              <ScriptProjectCard
                key={item.projectId}
                project={item}
                primaryCta={{
                  label: primaryCta.label,
                  to: primaryCta.to(item.projectId),
                }}
                secondaryCta={
                  secondaryCta
                    ? { label: secondaryCta.label, to: secondaryCta.to(item.projectId) }
                    : undefined
                }
              />
            ))}
          </div>
        ) : (
          <EmptyState title={emptyState.title} description={emptyState.description}>
            <Link className="nav-btn primary" to="/script-projects/new">
              {newLabel}
            </Link>
          </EmptyState>
        )}
      </div>
    </section>
  )
}
