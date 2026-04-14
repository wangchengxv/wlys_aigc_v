import { NavLink } from 'react-router-dom'

type Props = {
  projectId: string
}

const steps: Array<{
  label: string
  href: (projectId: string) => string
  end?: boolean
}> = [
  { label: '剧本与故事', href: (id) => `/script-projects/${id}/preview`, end: true },
  { label: '场景与道具', href: (id) => `/script-projects/${id}/assets`, end: true },
  { label: '镜头拆分与视频生成', href: (id) => `/script-projects/${id}/video`, end: true },
  { label: '成片与导出', href: (id) => `/script-projects/${id}/export`, end: true },
  { label: '提示词模板', href: (id) => `/script-projects/${id}/prompt-templates`, end: true },
]

export function ScriptProjectWorkflowNav({ projectId }: Props) {
  return (
    <nav className="script-project-workflow-nav panel glass" aria-label="剧本工作流">
      <p className="script-project-workflow-nav__title">工作流</p>
      <ul className="script-project-workflow-nav__list" role="list">
        {steps.map((step) => (
          <li key={step.label}>
            <NavLink
              to={step.href(projectId)}
              className={({ isActive }) => `script-project-workflow-nav__link${isActive ? ' is-active' : ''}`}
              end={step.end}
            >
              {step.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
