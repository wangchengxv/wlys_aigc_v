import { NavLink } from 'react-router-dom'

type Props = {
  projectId: string
}

const workflowItems: Array<{ label: string; to: (projectId: string) => string; end?: boolean }> = [
  { label: '总览', to: (id) => `/script-projects/${id}`, end: true },
  { label: '剧本', to: (id) => `/script-projects/${id}/preview`, end: true },
  { label: '资产', to: (id) => `/script-projects/${id}/assets`, end: true },
  { label: '视频', to: (id) => `/script-projects/${id}/video`, end: true },
  { label: '配音', to: (id) => `/script-projects/${id}/dubbing`, end: true },
  { label: '口型', to: (id) => `/script-projects/${id}/lip-sync`, end: true },
  { label: '剪辑', to: (id) => `/script-projects/${id}/final-composition`, end: true },
  { label: '导出', to: (id) => `/script-projects/${id}/export`, end: true },
]

export function ScriptProjectWorkflowNav({ projectId }: Props) {
  return (
    <div className="project-workspace-nav">
      <div className="project-workspace-nav__head">
        <div>
          <p className="project-workspace-nav__eyebrow">项目流程</p>
          <strong>项目工作区</strong>
        </div>
        <span className="project-workspace-nav__id">{projectId}</span>
      </div>

      <div className="project-workspace-nav__tabs">
        {workflowItems.map((item) => (
          <NavLink
            key={item.label}
            to={item.to(projectId)}
            end={item.end}
            className={({ isActive }) => `project-workspace-nav__tab${isActive ? ' is-active' : ''}`}
          >
            {item.label}
          </NavLink>
        ))}
        <NavLink to={`/audit-logs?entityType=SCRIPT_PROJECT&entityId=${encodeURIComponent(projectId)}`} className="project-workspace-nav__tab">
          审计
        </NavLink>
      </div>
    </div>
  )
}
