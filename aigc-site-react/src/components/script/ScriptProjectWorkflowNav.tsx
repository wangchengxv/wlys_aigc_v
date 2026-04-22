import { NavLink, useLocation } from 'react-router-dom'
import { useMemo, useCallback, memo } from 'react'

type Props = {
  projectId: string
}

type StepState = 'completed' | 'active' | 'pending'

type WorkflowStep = {
  key: string
  label: string
  to: (projectId: string) => string
  hint: string
}

const workflowSteps: WorkflowStep[] = [
  { key: 'overview', label: '总览', to: (id) => `/script-projects/${id}`, hint: '项目全局状态' },
  { key: 'script', label: '剧本', to: (id) => `/script-projects/${id}/preview`, hint: '完善剧本内容' },
  { key: 'assets', label: '资产', to: (id) => `/script-projects/${id}/assets`, hint: '生成角色道具' },
  { key: 'video', label: '视频', to: (id) => `/script-projects/${id}/video`, hint: '生成视频分段' },
  { key: 'dubbing', label: '配音', to: (id) => `/script-projects/${id}/dubbing`, hint: '添加语音旁白' },
  { key: 'lip-sync', label: '口型', to: (id) => `/script-projects/${id}/lip-sync`, hint: '同步口型动画' },
  { key: 'edit', label: '剪辑', to: (id) => `/script-projects/${id}/final-composition`, hint: '合成最终成片' },
  { key: 'export', label: '导出', to: (id) => `/script-projects/${id}/export`, hint: '打包交付文件' },
]

const getStepState = (index: number, path: string, currentIndex: number): StepState => {
  if (currentIndex === -1) return 'pending'
  if (index < currentIndex) return 'completed'
  if (index === currentIndex) return 'active'
  return 'pending'
}

const CheckIcon = memo(() => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="20 6 9 17 4 12" />
  </svg>
))

const ArrowIcon = memo(() => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="9 18 15 12 9 6" />
  </svg>
))

const InfoIcon = memo(() => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" />
    <path d="M12 16v-4" />
    <path d="M12 8h.01" />
  </svg>
))

const LayersIcon = memo(() => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 2L2 7l10 5 10-5-10-5z" />
    <path d="M2 17l10 5 10-5" />
    <path d="M2 12l10 5 10-5" />
  </svg>
))

const ShieldIcon = memo(() => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  </svg>
))

interface StepItemProps {
  step: typeof workflowSteps[0] & { state: StepState; index: number }
  projectId: string
}

const StepItem = memo(function StepItem({ step, projectId }: StepItemProps) {
  const isLast = step.index === workflowSteps.length - 1

  return (
    <li className={`wf-step wf-step--${step.state}`}>
      <NavLink
        to={step.to(projectId)}
        className={({ isActive }) => `wf-step__link${isActive ? ' is-active' : ''}`}
      >
        <div className="wf-step__indicator">
          <div className="wf-step__dot">
            {step.state === 'completed' ? (
              <CheckIcon />
            ) : (
              <span className="wf-step__num">{step.index + 1}</span>
            )}
          </div>
          {!isLast && (
            <div className={`wf-step__line ${step.state === 'completed' ? 'is-filled' : ''}`} />
          )}
        </div>

        <div className="wf-step__body">
          <span className="wf-step__label">{step.label}</span>
          {step.state === 'active' && step.hint && (
            <span className="wf-step__hint">{step.hint}</span>
          )}
        </div>

        <div className="wf-step__chevron">
          <ArrowIcon />
        </div>
      </NavLink>
    </li>
  )
})

export function ScriptProjectWorkflowNav({ projectId }: Props) {
  const location = useLocation()

  const currentIndex = useMemo(() => {
    return workflowSteps.findIndex((step) => {
      const targetPath = step.to('').replace(/\/$/, '')
      return location.pathname.includes(targetPath)
    })
  }, [location.pathname])

  const completedCount = useMemo(() => {
    return currentIndex > 0 ? currentIndex : 0
  }, [currentIndex])

  const progressPercent = useMemo(() => {
    return (completedCount / (workflowSteps.length - 1)) * 100
  }, [completedCount])

  const stepsWithState = useMemo(() => {
    return workflowSteps.map((step, index) => ({
      ...step,
      state: getStepState(index, location.pathname, currentIndex),
      index,
    }))
  }, [location.pathname, currentIndex])

  const currentStep = stepsWithState.find((s) => s.state === 'active')

  return (
    <nav className="wf-sidebar">
      <div className="wf-sidebar__head">
        <div className="wf-sidebar__icon">
          <LayersIcon />
        </div>
        <div className="wf-sidebar__meta">
          <p className="wf-sidebar__label">生产流程</p>
          <strong>项目工作区</strong>
        </div>
      </div>

      <div className="wf-progress">
        <div className="wf-progress__track">
          <div
            className="wf-progress__fill"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
        <span className="wf-progress__count">
          {completedCount}/{workflowSteps.length - 1}
        </span>
      </div>

      <ol className="wf-steps">
        {stepsWithState.map((step) => (
          <StepItem key={step.key} step={step} projectId={projectId} />
        ))}
      </ol>

      {currentStep && (
        <div className="wf-guide">
          <div className="wf-guide__icon">
            <InfoIcon />
          </div>
          <div className="wf-guide__text">
            <strong>当前：{currentStep.label}</strong>
            <span>{currentStep.hint}</span>
          </div>
        </div>
      )}

      <div className="wf-sidebar__foot">
        <NavLink
          to={`/audit-logs?entityType=SCRIPT_PROJECT&entityId=${encodeURIComponent(projectId)}`}
          className="wf-audit-link"
        >
          <ShieldIcon />
          查看审计记录
        </NavLink>
      </div>
    </nav>
  )
}